package org.wpfy.carlauncher.feature.media

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * What the head unit's own Bluetooth stack (A2DP *sink* / AVRCP *controller*) reports about the
 * phone that is streaming music to the car.
 */
internal data class BluetoothAudioState(
    val connected: Boolean = false,
    val streaming: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
)

/**
 * Listens to the Bluetooth media broadcasts used by car head units, so the launcher notices
 * that music started playing over Bluetooth without the user opening the Bluetooth music app.
 *
 * Only the sink/controller roles are observed on purpose: A2DP *source* broadcasts describe the
 * head unit streaming to a headset and must never be mistaken for phone music.
 *
 * Vendor firmware sometimes uses its own actions. Everything received is logged under
 * [LogTag] so the exact action/package names of a given unit can be added to [Actions].
 */
internal class BluetoothMediaBridge(
    context: Context,
    private val onChanged: () -> Unit,
) {
    private val appContext = context.applicationContext
    private var registered = false

    @Volatile
    var state: BluetoothAudioState = BluetoothAudioState()
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handle(intent)
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply { Actions.forEach { addAction(it) } }
        registered = runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            true
        }.getOrElse { error ->
            Log.w(LogTag, "Bluetooth media receiver not registered", error)
            false
        }
    }

    fun stop() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(receiver) }
        registered = false
        state = BluetoothAudioState()
    }

    @Suppress("DEPRECATION")
    private fun handle(intent: Intent) {
        val action = intent.action ?: return
        val previous = state
        var next = previous

        when (action) {
            ActionA2dpSinkConnection, ActionAvrcpConnection -> {
                val connected = intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED,
                ) == BluetoothProfile.STATE_CONNECTED
                next = if (connected) previous.copy(connected = true) else BluetoothAudioState()
            }

            ActionA2dpSinkPlaying -> {
                val playing = intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothA2dp.STATE_NOT_PLAYING,
                ) == BluetoothA2dp.STATE_PLAYING
                next = previous.copy(connected = previous.connected || playing, streaming = playing)
            }

            ActionAvrcpTrackEvent -> {
                val metadata = runCatching {
                    intent.getParcelableExtra<MediaMetadata>(ExtraAvrcpMetadata)
                }.getOrNull()
                val playback = runCatching {
                    intent.getParcelableExtra<PlaybackState>(ExtraAvrcpPlayback)
                }.getOrNull()
                next = previous.copy(connected = true)
                if (metadata != null) {
                    next = next.copy(
                        title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                        artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                        album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
                        durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
                    )
                }
                if (playback != null) {
                    next = next.copy(
                        streaming = playback.state == PlaybackState.STATE_PLAYING,
                        positionMs = playback.position.coerceAtLeast(0L),
                    )
                }
            }

            ActionLegacyMetaChanged, ActionLegacyPlayStateChanged -> {
                // These are sent by every music app, so only trust them while Bluetooth audio is active.
                if (previous.connected || previous.streaming) {
                    next = previous.copy(
                        title = intent.getStringExtra("track") ?: previous.title,
                        artist = intent.getStringExtra("artist") ?: previous.artist,
                        album = intent.getStringExtra("album") ?: previous.album,
                        streaming = if (intent.hasExtra("playing")) {
                            intent.getBooleanExtra("playing", previous.streaming)
                        } else {
                            previous.streaming
                        },
                    )
                }
            }
        }

        Log.d(LogTag, "action=$action extras=${intent.extras?.keySet()} -> $next")
        if (next != previous) {
            state = next
            onChanged()
        }
    }

    private companion object {
        const val ActionA2dpSinkConnection = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED"
        const val ActionA2dpSinkPlaying = "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED"
        const val ActionAvrcpConnection = "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED"
        const val ActionAvrcpTrackEvent = "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT"
        const val ExtraAvrcpMetadata = "android.bluetooth.avrcp-controller.profile.extra.METADATA"
        const val ExtraAvrcpPlayback = "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK"
        const val ActionLegacyMetaChanged = "com.android.music.metachanged"
        const val ActionLegacyPlayStateChanged = "com.android.music.playstatechanged"

        val Actions = listOf(
            ActionA2dpSinkConnection,
            ActionA2dpSinkPlaying,
            ActionAvrcpConnection,
            ActionAvrcpTrackEvent,
            ActionLegacyMetaChanged,
            ActionLegacyPlayStateChanged,
        )
    }
}

internal const val LogTag = "CarLauncherBT"

/** Bluetooth music apps of common head units (the user's unit is probed with exactly these). */
internal val BluetoothMusicPackages = listOf(
    "com.syu.bt",
    "com.android.bluetooth",
    "com.car.bluetooth",
    "com.mtk.bluetooth",
)

private val BluetoothPackageHints = listOf("bluetooth", "btmusic", "bt.music", "a2dp", "avrcp", ".bt.")

/**
 * Heuristic for media sessions published by the head unit's Bluetooth music app
 * (for example `com.android.bluetooth` or the vendor's `...bluetooth...` / `...bt` packages).
 */
internal fun isBluetoothMediaPackage(packageName: String): Boolean {
    val name = packageName.lowercase(Locale.ROOT)
    return name in BluetoothMusicPackages || name.endsWith(".bt") || BluetoothPackageHints.any { hint -> name.contains(hint) }
}
