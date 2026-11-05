package org.wpfy.carlauncher.feature.map

/**
 * Single place to tune the look and the data source of the home-screen mini map.
 *
 * Primary source is MapTiler's dark street style (needs [MapTilerKey]). If the key is blank,
 * rejected (401/403) or out of quota (429), the map automatically falls back to the public
 * OpenStreetMap tiles, which are darkened on the fly. Tiles are cached on disk, so areas you
 * have already driven through keep working without internet access.
 */
internal object MapStyle {
    /** Start zoom (street level). The user can change it with pinch or the +/- buttons. */
    const val DefaultZoom = 17f
    const val MinZoom = 13f
    const val MaxZoom = 18.99f

    /** Extra enlargement of every tile (and so of lines and street names) on top of density. */
    const val TileScale = 1.15f

    /** Slightly deeper contrast/saturation for the MapTiler dark tiles (cheap colour matrix). */
    const val GradeMapTilerTiles = true

    /** Size of a source tile in pixels. */
    const val TileSizePx = 256

    /**
     * true  = while idle (no destination) the card shows a decorative "luxury" city map that
     *         always renders, needs no internet and moves with the car; the real street map
     *         appears automatically while navigating.
     * false = always show the real street map.
     */
    const val DecorativeWhenIdle = false

    /** A failed tile is not retried for this long. */
    const val RetryAfterMillis = 30_000L

    /** Providers require an identifying User-Agent. */
    const val UserAgent = "CarLauncher/1.3 (personal Android head-unit launcher)"

    /**
     * MapTiler API key (free plan). The repository must stay private because the key ends up
     * inside the APK; regenerate it in the MapTiler dashboard if it ever leaks. Leave blank to
     * use OpenStreetMap only.
     */
    const val MapTilerKey = "uOWW3WySlSsHmhVtFxpN"

    private const val MapTilerStyleId = "streets-v2-dark"

    enum class Source(
        val id: String,
        val attribution: String,
        /** Public OSM tiles are light, so they are inverted into a dark look when drawn. */
        val needsDarkFilter: Boolean,
    ) {
        MapTiler("mt", "© MapTiler © OpenStreetMap", false),
        OpenStreetMap("osm", "© OpenStreetMap contributors", true),
    }

    fun initialSource(): Source {
        return if (MapTilerKey.isBlank()) Source.OpenStreetMap else Source.MapTiler
    }

    fun tileUrl(source: Source, zoom: Int, x: Int, y: Int): String {
        return when (source) {
            Source.MapTiler ->
                "https://api.maptiler.com/maps/$MapTilerStyleId/$TileSizePx/$zoom/$x/$y.png?key=$MapTilerKey"
            Source.OpenStreetMap -> "https://tile.openstreetmap.org/$zoom/$x/$y.png"
        }
    }
}
