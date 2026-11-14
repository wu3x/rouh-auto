package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor

/** Arabic OSM place and road names for the currently visited region.
 * Stored on disk per small geographic cell; never fetches during every GPS fix.
 */
internal data class ArabicMapLabel(val name: String, val lat: Double, val lon: Double, val place: Boolean)

internal object ArabicMapLabels {
    private const val CellDegrees = 0.06
    private const val RefreshMillis = 7L * 24 * 60 * 60 * 1000
    private const val RetryMillis = 30L * 60 * 1000
    private const val Endpoint = "https://overpass-api.de/api/interpreter"
    private val mutex = Mutex()
    private val _labels = MutableStateFlow<List<ArabicMapLabel>>(emptyList())
    val labels = _labels.asStateFlow()
    private var lastAttemptKey = ""
    private var lastAttemptElapsed = 0L

    suspend fun update(context: Context, lat: Double, lon: Double) {
        if (lat !in 16.0..33.0 || lon !in 34.0..56.0) {
            _labels.value = emptyList()
            return
        }
        val row = floor(lat / CellDegrees).toInt()
        val col = floor(lon / CellDegrees).toInt()
        val key = "${row}_${col}"
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val dir = java.io.File(context.applicationContext.filesDir, "arabic_map_labels").apply { mkdirs() }
                val file = java.io.File(dir, "$key.json")
                val cached = runCatching { if (file.isFile) JSONObject(file.readText()) else null }.getOrNull()
                val cachedLabels = cached?.let(::parseLabels).orEmpty()
                _labels.value = cachedLabels
                val fresh = cached != null && System.currentTimeMillis() - cached.optLong("savedAt", 0) < RefreshMillis
                if (fresh) return@withLock
                val now = SystemClock.elapsedRealtime()
                if (lastAttemptKey == key && now - lastAttemptElapsed < RetryMillis) return@withLock
                lastAttemptKey = key
                lastAttemptElapsed = now
                val south = row * CellDegrees
                val west = col * CellDegrees
                val north = south + CellDegrees
                val east = west + CellDegrees
                val query = """
                    [out:json][timeout:25];
                    (
                      way["highway"]["name:ar"]($south,$west,$north,$east);
                      nwr["place"]["name:ar"]($south,$west,$north,$east);
                    );
                    out center;
                """.trimIndent()
                val fetched = runCatching { fetch(query) }.getOrNull() ?: return@withLock
                val parsed = parseLabels(fetched)
                val stored = JSONObject(fetched.toString()).put("savedAt", System.currentTimeMillis())
                runCatching {
                    val temp = java.io.File(dir, "$key.tmp")
                    temp.writeText(stored.toString())
                    if (!temp.renameTo(file)) {
                        file.writeText(stored.toString())
                        temp.delete()
                    }
                    // Bound local disk usage to the most recently visited 150 regions.
                    dir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() }
                        ?.drop(150)?.forEach { it.delete() }
                }
                _labels.value = parsed
            }
        }
    }

    private fun fetch(query: String): JSONObject {
        val connection = (URL(Endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("User-Agent", "ROUH-Auto/1.0 (Android map labels; github.com/wu3x/rouh-auto)")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        return try {
            val body = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) error("Overpass HTTP ${connection.responseCode}")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            require(text.length <= 3_000_000) { "Map label response too large" }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLabels(json: JSONObject): List<ArabicMapLabel> {
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val output = ArrayList<ArabicMapLabel>()
        val seen = HashSet<String>()
        for (i in 0 until elements.length()) {
            val element = elements.optJSONObject(i) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val name = tags.optString("name:ar").trim()
            if (name.isBlank() || !name.any { it in '\u0600'..'\u06FF' }) continue
            val center = element.optJSONObject("center") ?: element
            val lat = center.optDouble("lat", Double.NaN)
            val lon = center.optDouble("lon", Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) continue
            val place = tags.has("place")
            val dedupe = "$name/${(lat * 1000).toInt()}/${(lon * 1000).toInt()}"
            if (seen.add(dedupe)) output.add(ArabicMapLabel(name.take(65), lat, lon, place))
            if (output.size >= 800) break
        }
        return output
    }
}
