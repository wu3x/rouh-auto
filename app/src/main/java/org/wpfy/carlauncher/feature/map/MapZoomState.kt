package org.wpfy.carlauncher.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** Zoom of the home map; process-wide so it survives switching screens inside the launcher. */
internal object MapZoomState {
    var zoom by mutableFloatStateOf(MapStyle.DefaultZoom)
        private set

    fun setZoom(level: Float) {
        zoom = level.coerceIn(MapStyle.MinZoom, MapStyle.MaxZoom)
    }

    fun zoomBy(levels: Float) {
        zoom = (zoom + levels).coerceIn(MapStyle.MinZoom, MapStyle.MaxZoom)
    }
}
