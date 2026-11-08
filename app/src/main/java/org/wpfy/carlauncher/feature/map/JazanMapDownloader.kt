package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Downloads the official raster MBTiles pack then passes it through the same validator as manual import. */
internal object JazanMapDownloader {
    // Publish the map as an asset named jazan-offline-test.mbtiles under release tag saudi-maps.
    private const val MAP_URL =
        "https://github.com/wu3x/rouh-auto/releases/download/saudi-maps/jazan-offline-test.mbtiles"
    private const val MAX_BYTES = 512L * 1024 * 1024

    suspend fun download(context: Context, progress: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val temp = File(app.cacheDir, "jazan-download.part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(MAP_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 25_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ROUH-Auto/1.3")
            }
            val code = connection.responseCode
            if (code != 200) {
                error(if (code == 404) "خريطة جازان لم تُنشر بعد، أو رابطها غير متاح للعامة" else "تعذر التنزيل: HTTP " + code)
            }
            val length = connection.contentLengthLong
            require(length <= MAX_BYTES) { "حجم الملف يتجاوز الحد المسموح" }
            var count = 0L
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        count += n
                        require(count <= MAX_BYTES) { "الملف كبير جدًا" }
                        output.write(buffer, 0, n)
                        if (length > 0 && count % (1024 * 1024) < n) {
                            withContext(Dispatchers.Main) { progress("تنزيل جازان: " + count * 100 / length + "%") }
                        }
                    }
                }
            }
            require(count > 4096) { "ملف التنزيل فارغ" }
            val result = OfflineMbtiles.install(app, Uri.fromFile(temp))
            MapZoomState.updateZoomLevel(14f)
            result
        } finally {
            connection?.disconnect()
            temp.delete()
        }
    }
}
