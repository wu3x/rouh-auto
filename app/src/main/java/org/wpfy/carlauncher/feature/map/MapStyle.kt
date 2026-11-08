package org.wpfy.carlauncher.feature.map

/**
 * ROUH Auto map presentation and tile source configuration.
 * Offline MBTiles take priority; OpenStreetMap is used when network access is available.
 */
internal object MapStyle {
    const val DefaultZoom = 17f
    const val MinZoom = 9f
    const val MaxZoom = 18.99f
    const val TileScale = 1.15f
    const val TileSizePx = 256
    const val DecorativeWhenIdle = false
    const val RetryAfterMillis = 30_000L
    const val UserAgent = "ROUH-Auto/1.3 (Android car launcher)"

    enum class Source(
        val id: String,
        val attribution: String,
        val needsDarkFilter: Boolean,
    ) {
        OpenStreetMap("osm", "© OpenStreetMap contributors", true),
    }

    fun initialSource(): Source = Source.OpenStreetMap

    fun tileUrl(source: Source, zoom: Int, x: Int, y: Int): String =
        "https://tile.openstreetmap.org/$zoom/$x/$y.png"
}
