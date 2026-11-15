package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
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
    private val Endpoints = listOf("https://overpass.kumi.systems/api/interpreter", "https://overpass-api.de/api/interpreter")
    private val _downloadStatus = MutableStateFlow("جاهز لتحميل الأسماء")
    val downloadStatus = _downloadStatus.asStateFlow()
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()
    private val _downloadBusy = MutableStateFlow(false)
    val downloadBusy = _downloadBusy.asStateFlow()
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
                _labels.value = nearbyLabels(dir, row, col).ifEmpty { cachedLabels }
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
                _labels.value = nearbyLabels(dir, row, col).ifEmpty { parsed }
            }
        }
    }

    /** Download names for areas where map tile PNGs have actually been cached.
     * Limited batch size avoids overwhelming public Overpass servers.
     */
    suspend fun downloadForCachedMap(context: Context, currentLat: Double?, currentLon: Double?) {
        if (_downloadBusy.value) return
        _downloadBusy.value = true
        _downloadProgress.value = 0f
        try {
            val cells = withContext(Dispatchers.IO) {
                val selected = linkedSetOf<Pair<Int, Int>>()
                if (currentLat != null && currentLon != null && currentLat in -85.0..85.0 && currentLon in -180.0..180.0) {
                    selected.add(floor(currentLat / CellDegrees).toInt() to floor(currentLon / CellDegrees).toInt())
                }
                val files = java.io.File(context.filesDir, "map_tiles").listFiles().orEmpty()
                files.filter { it.extension == "png" }.sortedByDescending { it.lastModified() }.forEach { file ->
                    val match = Regex("""^osm_([0-9]+)_([0-9]+)_([0-9]+)[.]png$""").matchEntire(file.name) ?: return@forEach
                    val z = match.groupValues[1].toIntOrNull() ?: return@forEach
                    val x = match.groupValues[2].toIntOrNull() ?: return@forEach
                    val y = match.groupValues[3].toIntOrNull() ?: return@forEach
                    if (z !in 10..18) return@forEach
                    val n = 2.0.pow(z)
                    val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(Math.PI * (1.0 - 2.0 * (y + 0.5) / n))))
                    val lon = (x + 0.5) / n * 360.0 - 180.0
                    if (lat in -85.0..85.0 && lon in -180.0..180.0) selected.add(floor(lat / CellDegrees).toInt() to floor(lon / CellDegrees).toInt())
                }
                selected.toList()
            }
            if (cells.isEmpty()) {
                _downloadStatus.value = "ما لقينا أجزاء خريطة محفوظة. افتح الخريطة مع الإنترنت أولًا."
                return
            }
            val dir = java.io.File(context.filesDir, "arabic_map_labels").apply { mkdirs() }
            val pending = cells.filter { (r, c) -> !java.io.File(dir, "${r}_${c}.json").isFile }
            if (pending.isEmpty()) {
                _downloadProgress.value = 1f
                _downloadStatus.value = "كل مناطق الخريطة المحفوظة (${cells.size}) لها ملفات أسماء. ما فيه مناطق جديدة."
                return
            }
            val batch = pending.take(12)
            var succeeded = 0
            var failed = 0
            batch.forEachIndexed { i, (row, col) ->
                _downloadStatus.value = "تحميل أسماء المنطقة ${i + 1} من ${batch.size} (المتبقي من المناطق: ${pending.size - i})"
                val south = row * CellDegrees
                val west = col * CellDegrees
                val query = """[out:json][timeout:25];(nwr["place"]($south,$west,${south + CellDegrees},${west + CellDegrees});way["highway"]["name"]($south,$west,${south + CellDegrees},${west + CellDegrees}););out center;"""
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val json = fetch(query)
                        val parsed = parseLabels(json)
                        val file = java.io.File(dir, "${row}_${col}.json")
                        val tmp = java.io.File(dir, "${row}_${col}.tmp")
                        tmp.writeText(json.put("savedAt", System.currentTimeMillis()).toString())
                        if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
                        parsed.size
                    }
                }
                if (result.isSuccess) succeeded++ else failed++
                _downloadProgress.value = (i + 1f) / batch.size
            }
            _downloadStatus.value = "اكتمل: $succeeded منطقة محفوظة، $failed فشلت. باقي ${pending.size - batch.size} منطقة؛ اضغط مجددًا للمتابعة."
            if (currentLat != null && currentLon != null) loadCached(context, currentLat, currentLon)
        } finally {
            _downloadBusy.value = false
        }
    }

    suspend fun loadCached(context: Context, lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        val row = floor(lat / CellDegrees).toInt()
        val col = floor(lon / CellDegrees).toInt()
        val dir = java.io.File(context.filesDir, "arabic_map_labels")
        _labels.value = nearbyLabels(dir, row, col)
    }

    private fun nearbyLabels(dir: java.io.File, row: Int, col: Int): List<ArabicMapLabel> {
        val all = ArrayList<ArabicMapLabel>()
        for (r in row - 1..row + 1) for (c in col - 1..col + 1) {
            val f = java.io.File(dir, "${r}_${c}.json")
            if (f.isFile) runCatching { all.addAll(parseLabels(JSONObject(f.readText()))) }
        }
        return all.distinctBy { "${it.name}/${(it.lat * 10000).toInt()}/${(it.lon * 10000).toInt()}" }.take(1200)
    }

    private fun fetch(query: String): JSONObject {
        var last: Exception? = null
        for (endpoint in Endpoints) {
            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("User-Agent", "ROUH-Auto/1.3 (Android map labels; github.com/wu3x/rouh-auto)")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                }
                try {
                    val body = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode != 200) error("Overpass HTTP ${connection.responseCode}")
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    require(text.length <= 3_000_000) { "Map label response too large" }
                    return JSONObject(text)
                } finally { connection.disconnect() }
            } catch (e: Exception) { last = e }
        }
        throw last ?: java.io.IOException("No Overpass server available")
    }

    private fun parseLabels(json: JSONObject): List<ArabicMapLabel> {
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val output = ArrayList<ArabicMapLabel>()
        val seen = HashSet<String>()
        for (i in 0 until elements.length()) {
            val element = elements.optJSONObject(i) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val name = tags.optString("name:ar").ifBlank { tags.optString("name") }.trim()
            if (name.isBlank()) continue
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
