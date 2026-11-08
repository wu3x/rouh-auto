package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Downloads the official raster MBTiles pack then passes it through the same validator as manual import. */
internal object JazanMapDownloader {
    // Publish the map as an asset named jazan-offline-test.mbtiles under release tag saudi-maps.
    private const val MAP_URL =
        "https://github.com/wu3x/rouh-auto/releases/download/saudi-maps/jazan-offline-test.mbtiles"
    private const val MAX_BYTES = 512L * 1024 * 1024
    private const val EXPECTED_SHA256 = "d505a1e8542c404c3eff08fdab0404b76dad63615c02e5140e70bc97fdf57a32"

    suspend fun download(context: Context, progress: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val temp = File(app.cacheDir, "jazan-download.part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            val sources = listOf(
                MAP_URL,
                "https://api.github.com/repos/wu3x/rouh-auto/releases/assets/723295855",
            )
            var downloaded = false
            var lastFailure: Exception? = null

            for (attempt in 1..4) {
                val source = sources[(attempt - 1) % sources.size]
                var connection: HttpURLConnection? = null
                try {
                    withContext(Dispatchers.Main) {
                        progress("الاتصال بخادم الخريطة (المحاولة $attempt من 4)...")
                    }
                    connection = (URL(source).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 45_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "ROUH-Auto/1.3")
                        if (source.contains("api.github.com")) {
                            setRequestProperty("Accept", "application/octet-stream")
                            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                        }
                    }
                    val code = connection.responseCode
                    if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
                    val contentType = connection.contentType.orEmpty()
                    if (contentType.contains("json", ignoreCase = true)) {
                        throw IOException("الخادم أعاد بيانات بدلاً من ملف الخريطة")
                    }
                    val length = connection.contentLengthLong
                    require(length <= MAX_BYTES) { "حجم الملف يتجاوز الحد المسموح" }
                    digest.reset()
                    count = 0L
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(32768)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                count += n
                                require(count <= MAX_BYTES) { "الملف كبير جدًا" }
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                if (length > 0 && count % (1024 * 1024) < n) {
                                    withContext(Dispatchers.Main) {
                                        progress("تنزيل جازان: " + count * 100 / length + "%")
                                    }
                                }
                            }
                        }
                    }
                    require(count > 4096) { "ملف التنزيل فارغ" }
                    downloaded = true
                    break
                } catch (error: IOException) {
                    lastFailure = error
                    temp.delete()
                    if (attempt < 4) {
                        kotlinx.coroutines.delay(attempt * 1500L)
                    }
                } finally {
                    connection?.disconnect()
                }
            }
            if (!downloaded) {
                throw IOException("تعذر الاتصال بخوادم التنزيل بعد 4 محاولات: ${lastFailure?.message}")
            }
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            require(checksum == EXPECTED_SHA256) { "ملف الخريطة لا يطابق النسخة المعتمدة، أوقفنا التثبيت لحمايتك" }
            val result = OfflineMbtiles.install(app, Uri.fromFile(temp))
            MapZoomState.updateZoomLevel(14f)
            result
        } finally {
            temp.delete()
        }
    }
}
