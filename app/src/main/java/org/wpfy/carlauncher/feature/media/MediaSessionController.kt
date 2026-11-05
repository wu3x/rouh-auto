package org.wpfy.carlauncher.feature.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Rating
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.view.KeyEvent

class MediaSessionController(
    private val currentController: () -> MediaController?,
    /**
     * Used when the selected source has no [MediaController] (Bluetooth music played by the
     * head unit's own stack): dispatches the media key and returns true if it was handled.
     */
    private val mediaKeyFallback: ((keyCode: Int) -> Boolean)? = null,
) {
    fun playPause() {
        val controller = currentController()
        if (controller == null) {
            mediaKeyFallback?.invoke(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            return
        }
        val playbackState = controller.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING ||
            playbackState?.state == PlaybackState.STATE_BUFFERING

        if (isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun previous() {
        val controller = currentController()
        if (controller == null) {
            mediaKeyFallback?.invoke(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        } else {
            controller.transportControls.skipToPrevious()
        }
    }

    fun next() {
        val controller = currentController()
        if (controller == null) {
            mediaKeyFallback?.invoke(KeyEvent.KEYCODE_MEDIA_NEXT)
        } else {
            controller.transportControls.skipToNext()
        }
    }

    fun seekTo(positionMs: Long) {
        currentController()?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleRepeat() {
        val controller = currentController() ?: return
        val repeatAction = controller.playbackState.customActionMatching("repeat") ?: return
        controller.transportControls.sendCustomAction(repeatAction.action, null)
    }

    fun toggleShuffle() {
        val controller = currentController() ?: return
        val shuffleAction = controller.playbackState.customActionMatching("shuffle") ?: return
        controller.transportControls.sendCustomAction(shuffleAction.action, null)
    }

    fun toggleFavorite() {
        val controller = currentController() ?: return
        val playbackState = controller.playbackState
        val supportsHeartRating = controller.ratingType == Rating.RATING_HEART &&
            playbackState.hasAction(PlaybackState.ACTION_SET_RATING)

        if (supportsHeartRating) {
            val currentRating = controller.metadata
                ?.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)
            controller.transportControls.setRating(
                Rating.newHeartRating(currentRating?.hasHeart() != true),
            )
            return
        }

        val favoriteAction = playbackState.favoriteCustomAction() ?: return
        controller.transportControls.sendCustomAction(favoriteAction.action, null)
    }

    fun openSource(context: Context): Boolean {
        val controller = currentController() ?: return false
        if (sendSessionActivity(controller.sessionActivity)) {
            return true
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(controller.packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    private fun sendSessionActivity(pendingIntent: PendingIntent?): Boolean {
        if (pendingIntent == null) return false
        return runCatching {
            pendingIntent.send()
            true
        }.getOrDefault(false)
    }
}

internal fun PlaybackState?.hasAction(action: Long): Boolean {
    return this != null && actions and action != 0L
}

internal fun PlaybackState?.favoriteCustomAction(): PlaybackState.CustomAction? {
    return customActionMatching("favorite", "favourite", "heart", "like", "save")
}

internal fun PlaybackState?.customActionMatching(vararg terms: String): PlaybackState.CustomAction? {
    return this?.customActions?.firstOrNull { customAction ->
        val searchable = "${customAction.action} ${customAction.name}".lowercase()
        terms.any(searchable::contains)
    }
}
