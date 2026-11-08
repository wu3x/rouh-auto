package org.wpfy.carlauncher.feature.map

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Read-only MBTiles (TMS rows converted from XYZ). */
internal object OfflineMbtiles {
    private const val FILE = "jazan-offline.mbtiles"
    private val lock = Any()
    private var database: SQLiteDatabase? = null

    fun installed(context: Context) = File(context.filesDir, FILE).isFile

    fun tile(context: Context, z: Int, x: Int, y: Int): ByteArray? {
        if (z !in 0..22 || x < 0 || y < 0 || x.toLong() >= (1L shl z) || y.toLong() >= (1L shl z)) return null
        synchronized(lock) {
            try {
                if (!installed(context)) return null
                if (database?.isOpen != true) {
                    database = SQLiteDatabase.openDatabase(File(context.filesDir, FILE).path, null, SQLiteDatabase.OPEN_READONLY)
                }
                val tmsY = ((1L shl z) - 1 - y).toString()
                database!!.rawQuery(
                    "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
                    arrayOf(z.toString(), x.toString(), tmsY)
                ).use { c -> return if (c.moveToFirst()) c.getBlob(0) else null }
            } catch (_: Exception) {
                database?.close()
                database = null
                return null
            }
        }
    }

    suspend fun install(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val destination = File(app.filesDir, FILE)
        val temp = File(app.filesDir, "jazan-offline.part")
        try {
            var size = 0L
            val input = app.contentResolver.openInputStream(uri) ?: error("تعذر فتح الملف")
            input.use { source ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        size += n
                        require(size <= 512L * 1024 * 1024) { "الملف كبير جدًا" }
                        output.write(buffer, 0, n)
                    }
                }
            }
            require(size > 4096) { "ملف غير صالح" }
            SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val tables = mutableSetOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    while (c.moveToNext()) tables.add(c.getString(0))
                }
                require("tiles" in tables && "metadata" in tables) { "ليس ملف MBTiles" }
                db.rawQuery("SELECT COUNT(*) FROM tiles", null).use { c ->
                    require(c.moveToFirst() && c.getLong(0) > 0) { "الخريطة فارغة" }
                }
                db.rawQuery("SELECT value FROM metadata WHERE name='format'", null).use { c ->
                    require(c.moveToFirst() && c.getString(0).lowercase() in setOf("jpg", "jpeg", "png", "webp")) { "صيغة غير مدعومة" }
                }
            }
            synchronized(lock) {
                database?.close()
                database = null
                if (destination.exists() && !destination.delete()) error("تعذر استبدال الملف")
                require(temp.renameTo(destination)) { "تعذر حفظ الملف" }
            }
            MapTileStore.offlineMapChanged()
            "تم تثبيت خريطة جازان دون إنترنت"
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }
}
