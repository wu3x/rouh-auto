package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * A deliberately bounded installer for raster tile packs created by the project.
 * Expected ZIP entries: osm_z_x_y.png (matching MapTileStore's disk cache keys).
 * Publishing a real, licensed pack is a separate prerequisite.
 */
object SaudiMapPackageDownloader {
    private const val URL_JEDDAH =
        "https://github.com/wu3x/rouh-auto/releases/download/saudi-maps/jeddah-tiles.zip"
    private const val MAX_ARCHIVE = 200L * 1024 * 1024
    private const val MAX_UNPACKED = 400L * 1024 * 1024
    private val tileName = Regex("osm_([0-9]{1,2})_([0-9]+)_([0-9]+)\\.png")
    private val main = Handler(Looper.getMainLooper())

    fun downloadJeddah(context: Context, onStatus: (String) -> Unit) {
        val app = context.applicationContext
        fun notify(message: String) = main.post { onStatus(message) }
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            val archive = File(app.cacheDir, "jeddah-tiles.part")
            try {
                connection = URL(URL_JEDDAH).openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "ROUH-Auto/1.3")
                val code = connection.responseCode
                if (code != 200) {
                    notify(if (code == 404) "خريطة جدة لم تُنشر بعد على GitHub Releases" else "تعذر جلب الخريطة (HTTP $code)")
                    return@launch
                }
                val total = connection.contentLengthLong
                if (total > MAX_ARCHIVE) {
                    notify("ملف الخريطة يتجاوز الحد المسموح")
                    return@launch
                }
                var downloaded = 0L
                connection.inputStream.use { input ->
                    archive.outputStream().use { output ->
                        val buffer = ByteArray(32768)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            downloaded += n
                            if (downloaded > MAX_ARCHIVE) throw IllegalStateException("الحزمة كبيرة جدًا")
                            output.write(buffer, 0, n)
                            if (total > 0 && downloaded % (1024 * 1024) < n) {
                                notify("تنزيل جدة: ${downloaded * 100 / total}%")
                            }
                        }
                    }
                }
                val target = File(app.filesDir, "map_tiles").also { it.mkdirs() }
                val staging = File(app.cacheDir, "jeddah-map-staging")
                staging.deleteRecursively()
                staging.mkdirs()
                var extracted = 0L
                var count = 0
                ZipInputStream(archive.inputStream().buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val match = tileName.matchEntire(entry.name)
                        if (!entry.isDirectory && match != null) {
                            val (z, x, y) = match.destructured
                            val zoom = z.toInt()
                            val limit = if (zoom in 0..22) 1L shl zoom else 0L
                            if (x.toLong() < limit && y.toLong() < limit) {
                                val file = File(staging, entry.name)
                                file.outputStream().use { output ->
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val n = zip.read(buffer)
                                        if (n < 0) break
                                        extracted += n
                                        if (extracted > MAX_UNPACKED) throw IllegalStateException("حجم الملفات كبير جدًا")
                                        output.write(buffer, 0, n)
                                    }
                                }
                                count++
                            }
                        }
                        zip.closeEntry()
                    }
                }
                if (count == 0) throw IllegalStateException("الحزمة لا تحتوي بلاطات PNG متوافقة")
                staging.listFiles().orEmpty().forEach { file ->
                    file.copyTo(File(target, file.name), overwrite = true)
                }
                staging.deleteRecursively()
                notify("تم حفظ $count بلاطة لجدة. أعد فتح الخريطة لتحديث العرض.")
            } catch (e: Exception) {
                notify("تعذر تثبيت الخريطة: ${e.message ?: "خطأ غير معروف"}")
            } finally {
                archive.delete()
                connection?.disconnect()
            }
        }
    }
}
