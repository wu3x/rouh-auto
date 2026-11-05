package org.wpfy.carlauncher.feature.media

import android.graphics.Bitmap

enum class MediaAccessStatus {
    PermissionRequired,
    Available,
}

data class MediaSource(
    val packageName: String,
    val label: String,
    val icon: Bitmap? = null,
)

data class MediaControls(
    val canPlayPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
    val canRepeat: Boolean = false,
    val canShuffle: Boolean = false,
    val canFavorite: Boolean = false,
)

data class MediaSourceSession(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val source: MediaSource? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isRepeatEnabled: Boolean? = null,
    val isShuffleEnabled: Boolean? = null,
    val isFavorite: Boolean? = null,
    val controls: MediaControls = MediaControls(),
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class MediaState(
    val accessStatus: MediaAccessStatus = MediaAccessStatus.PermissionRequired,
    val hasSession: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val source: MediaSource? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isRepeatEnabled: Boolean? = null,
    val isShuffleEnabled: Boolean? = null,
    val isFavorite: Boolean? = null,
    val controls: MediaControls = MediaControls(),
    val availableSessions: List<MediaSourceSession> = emptyList(),
    val selectedSessionId: String? = null,
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val selectedSession: MediaSourceSession?
        get() = availableSessions.firstOrNull { it.id == selectedSessionId }
            ?: if (hasSession) {
                MediaSourceSession(
                    id = selectedSessionId ?: "selected",
                    title = title,
                    artist = artist,
                    artwork = artwork,
                    source = source,
                    durationMs = durationMs,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    isRepeatEnabled = isRepeatEnabled,
                    isShuffleEnabled = isShuffleEnabled,
                    isFavorite = isFavorite,
                    controls = controls,
                )
            } else {
                null
            }
}
