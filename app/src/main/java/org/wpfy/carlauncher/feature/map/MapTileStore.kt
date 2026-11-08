package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Process-wide tile store: small in-memory LRU (RGB_565, byte-bounded) in front of a
 * size-capped disk cache in front of the network.
 *
 * Memory use is bounded by [MemoryCacheBytes] no matter how long the car drives, downloads
 * are limited to [MaxParallelDownloads], and an [OutOfMemoryError] while decoding simply
 * drops the memory cache instead of crashing the launcher.
 */
internal object MapTileStore {
    private const val MemoryCacheBytes = 10 * 1024 * 1024
    private const val DiskCacheBytes = 512L * 1024L * 1024L
    private const val MaxTileFileBytes = 512 * 1024
    private const val MaxParallelDownloads = 3
    private const val TrimEveryNWrites = 100
    private const val MaxTrackedFailures = 2_000

    private val memory = object : LruCache<String, ImageBitmap>(MemoryCacheBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return value.asAndroidBitmap().byteCount
        }
    }

    private val lock = Any()
    private val inFlight = HashSet<String>()
    private val failedAt = HashMap<String, Long>()
    private val downloadPermits = Semaphore(MaxParallelDownloads)

    @Volatile
    private var diskDirectory: File? = null
    @Volatile private var appContext: Context? = null
    private var writesSinceTrim = 0

    private val _version = MutableStateFlow(0)
    private val _source = MutableStateFlow(MapStyle.initialSource())

    /** Online tile source used when no offline tile is available. */
    val source: StateFlow<MapStyle.Source> = _source.asStateFlow()

    /** Increments whenever a new tile becomes available so the map can redraw. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (diskDirectory != null) return
        synchronized(lock) {
            if (diskDirectory == null) {
                diskDirectory = File(context.applicationContext.filesDir, "map_tiles")
                    .also { it.mkdirs() }
            }
        }
    }

    /** Returns the tile only if it is already in memory. Cheap enough to call while drawing. */
    fun peek(zoom: Int, x: Int, y: Int): ImageBitmap? = memory.get("offline/$zoom/$x/$y") ?: memory.get(key(_source.value, zoom, x, y))

    fun offlineMapChanged() {
        memory.evictAll()
        _version.update { it + 1 }
    }

    fun isOfflineCached(zoom: Int, x: Int, y: Int): Boolean = memory.get("offline/$zoom/$x/$y") != null

    /** Loads a tile from disk or network in [scope]; no-op when already cached or in flight. */
    fun request(scope: CoroutineScope, zoom: Int, x: Int, y: Int) {
        // Never construct a URL or disk path for an invalid slippy-map tile.
        if (zoom !in 0..22 || x < 0 || y < 0 || x.toLong() >= (1L shl zoom) || y.toLong() >= (1L shl zoom)) return
        val source = _source.value
        val key = key(source, zoom, x, y)
        if (memory.get(key) != null) return

        synchronized(lock) {
            val failed = failedAt[key]
            if (failed != null && SystemClock.elapsedRealtime() - failed < MapStyle.RetryAfterMillis) {
                return
            }
            if (!inFlight.add(key)) return
        }

        scope.launch(Dispatchers.IO) {
            try {
                downloadPermits.withPermit {
                    val offline = appContext?.let { OfflineMbtiles.tile(it, zoom, x, y) }?.let { decode(it) }
                    val tile = offline ?: loadFromDisk(source, zoom, x, y) ?: download(source, zoom, x, y)
                    if (tile != null) {
                        memory.put(if (offline != null) "offline/$zoom/$x/$y" else key, tile)
                        synchronized(lock) { failedAt.remove(key) }
                        _version.update { it + 1 }
                    } else {
                        synchronized(lock) {
                            if (failedAt.size >= MaxTrackedFailures) failedAt.clear()
                            failedAt[key] = SystemClock.elapsedRealtime()
                        }
                    }
                }
            } finally {
                synchronized(lock) { inFlight.remove(key) }
            }
        }
    }

    private fun key(source: MapStyle.Source, zoom: Int, x: Int, y: Int) = "${source.id}/$zoom/$x/$y"

    private fun tileFile(source: MapStyle.Source, zoom: Int, x: Int, y: Int): File? {
        val directory = diskDirectory ?: return null
        return File(directory, "${source.id}_${zoom}_${x}_$y.png")
    }

    private fun loadFromDisk(source: MapStyle.Source, zoom: Int, x: Int, y: Int): ImageBitmap? {
        val file = tileFile(source, zoom, x, y) ?: return null
        if (!file.isFile) return null
        if (file.length() <= 0 || file.length() > MaxTileFileBytes) {
            file.delete()
            return null
        }
        val bytes = try {
            file.readBytes()
        } catch (_: IOException) {
            return null
        }
        val tile = decode(bytes)
        if (tile == null) {
            file.delete()
        } else {
            file.setLastModified(System.currentTimeMillis())
        }
        return tile
    }

    private fun download(source: MapStyle.Source, zoom: Int, x: Int, y: Int): ImageBitmap? {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(MapStyle.tileUrl(source, zoom, x, y)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", MapStyle.UserAgent)
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null

            // Bound the download while reading; readBytes() could allocate unbounded memory.
            if (connection.contentLengthLong > MaxTileFileBytes) return null
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MaxTileFileBytes) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            if (bytes.isEmpty()) return null

            val tile = decode(bytes) ?: return null
            writeToDisk(source, zoom, x, y, bytes)
            return tile
        } catch (_: IOException) {
            return null
        } catch (_: SecurityException) {
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun decode(bytes: ByteArray): ImageBitmap? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        } catch (_: OutOfMemoryError) {
            memory.evictAll()
            null
        }
    }

    private fun writeToDisk(source: MapStyle.Source, zoom: Int, x: Int, y: Int, bytes: ByteArray) {
        val file = tileFile(source, zoom, x, y) ?: return
        try {
            val partial = File(file.parentFile, file.name + ".part")
            partial.writeBytes(bytes)
            if (!partial.renameTo(file)) partial.delete()
        } catch (_: IOException) {
            return
        }

        val shouldTrim = synchronized(lock) {
            writesSinceTrim += 1
            (writesSinceTrim >= TrimEveryNWrites).also { if (it) writesSinceTrim = 0 }
        }
        if (shouldTrim) trimDisk()
    }

    private fun trimDisk() {
        val files = diskDirectory?.listFiles()?.filter { it.isFile && it.extension == "png" } ?: return
        var total = files.sumOf { it.length() }
        if (total <= DiskCacheBytes) return
        for (file in files.sortedBy(File::lastModified)) {
            if (total <= DiskCacheBytes * 3 / 4) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}
