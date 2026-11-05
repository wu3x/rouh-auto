package org.wpfy.carlauncher.feature.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** Web-Mercator helpers. World coordinates are normalised to the 0..1 range. */

internal fun lonToWorldX(longitude: Double): Double = (longitude + 180.0) / 360.0

internal fun latToWorldY(latitude: Double): Double {
    val clamped = latitude.coerceIn(-MaxMercatorLatitude, MaxMercatorLatitude)
    val radians = Math.toRadians(clamped)
    return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0
}

/** Ground resolution of a 256px tile at [zoom] for the given latitude. */
internal fun metersPerTilePixel(latitude: Double, zoom: Int): Double {
    return 156_543.03392 * cos(Math.toRadians(latitude)) / 2.0.pow(zoom)
}

private const val MaxMercatorLatitude = 85.05112878
