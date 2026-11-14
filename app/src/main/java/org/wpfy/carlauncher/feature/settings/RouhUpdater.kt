package org.wpfy.carlauncher.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class RouhRelease(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val sha256: String,
    val size: Long,
)

internal object RouhUpdater {
    private const val ReleaseApi = "https://api.github.com/repos/wu3x/rouh-auto/releases/tags/latest-signed"
    private const val ExpectedAsset = "rouh-auto-release.apk"
    private const val MaxApkBytes = 180L * 1024 * 1024

    @Suppress("DEPRECATION")
    fun installedCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    @Suppress("DEPRECATION")
    fun installedName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    suspend fun check(): RouhRelease = withContext(Dispatchers.IO) {
        val connection = open(ReleaseApi)
        try {
            require(connection.responseCode == 200) { "تعذر الاتصال بـ GitHub (${connection.responseCode})" }
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val body = release.optString("body")
            fun field(key: String): String = Regex("$key: ([^\\\\n\\r ]+)").find(body)?.groupValues?.get(1)
                ?.trim()?.trimEnd('\\') ?: ""
            val code = field("versionCode").toLongOrNull() ?: error("الإصدار المنشور ما فيه رقم بناء معتمد")
            val name = field("versionName").ifBlank { "1.3.$code" }
            val checksum = field("sha256").lowercase()
            require(checksum.matches(Regex("[0-9a-f]{64}"))) { "الإصدار المنشور ما فيه بصمة تحقق" }
            val assets = release.getJSONArray("assets")
            val asset = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name") == ExpectedAsset } ?: error("ملف التحديث غير موجود")
            val url = asset.getString("browser_download_url")
            require(url == "https://github.com/wu3x/rouh-auto/releases/download/latest-signed/$ExpectedAsset") {
                "رابط تحديث غير متوقع"
            }
            val size = asset.getLong("size")
            require(size in 1..MaxApkBytes) { "حجم التحديث غير صالح" }
            RouhRelease(code, name, url, checksum, size)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(context: Context, release: RouhRelease, progress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "updates").apply { mkdirs() }
            val temp = File(folder, "rouh-auto.part")
            val apk = File(folder, ExpectedAsset)
            temp.delete()
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                val connection = open(release.downloadUrl)
                try {
                    require(connection.responseCode == 200) { "تعذر تنزيل التحديث (${connection.responseCode})" }
                    var received = 0L
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(32768)
                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                received += count
                                require(received <= MaxApkBytes) { "الملف أكبر من الحد المسموح" }
                                digest.update(buffer, 0, count)
                                output.write(buffer, 0, count)
                                progress((received * 100 / release.size).toInt().coerceIn(0, 99))
                            }
                        }
                    }
                    require(received == release.size) { "التنزيل غير مكتمل" }
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    require(actual.equals(release.sha256, ignoreCase = true)) { "فشل التحقق من سلامة التحديث" }
                    if (apk.exists()) apk.delete()
                    require(temp.renameTo(apk)) { "تعذر حفظ ملف التحديث" }
                    progress(100)
                    apk
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                temp.delete()
                throw e
            }
        }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(6) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 35_000
                setRequestProperty("User-Agent", "ROUH-Auto-Updater")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode !in 300..399) return connection
            val next = connection.getHeaderField("Location") ?: error("رابط إعادة توجيه غير صالح")
            connection.disconnect()
            val parsed = URL(next)
            require(parsed.protocol == "https") { "الاتصال غير آمن" }
            require(parsed.host == "github.com" || parsed.host == "release-assets.githubusercontent.com" ||
                parsed.host == "objects.githubusercontent.com" || parsed.host == "api.github.com") {
                "وجهة تنزيل غير موثوقة"
            }
            current = next
        }
        error("عدد كبير من عمليات إعادة التوجيه")
    }
}
