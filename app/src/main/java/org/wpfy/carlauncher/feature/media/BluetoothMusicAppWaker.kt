package org.wpfy.carlauncher.feature.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.util.Log

/**
 * Many head units only start routing Bluetooth audio after their Bluetooth music app has been
 * opened. Connecting a [MediaBrowser] to that app's media service starts the app's process in
 * the background (no UI), so audio and its media session come up without opening the app.
 *
 * Only apps recognised by [isBluetoothMediaPackage] are touched. Call from the main thread.
 */
internal class BluetoothMusicAppWaker(context: Context) {
    private val appContext = context.applicationContext
    private val browsers = HashMap<String, MediaBrowser>()

    fun wake() {
        val services = runCatching {
            appContext.packageManager.queryIntentServices(Intent(MediaBrowserServiceAction), 0)
        }.getOrDefault(emptyList())

        val candidates = services
            .mapNotNull { it.serviceInfo }
            .filter { isBluetoothMediaPackage(it.packageName) }
        Log.d(LogTag, "Bluetooth media services: ${candidates.map { "${it.packageName}/${it.name}" }}")

        for (info in candidates) {
            val packageName = info.packageName
            if (browsers.containsKey(packageName)) continue

            val callback = object : MediaBrowser.ConnectionCallback() {
                override fun onConnectionFailed() {
                    browsers.remove(packageName)
                }

                override fun onConnectionSuspended() {
                    browsers.remove(packageName)
                }
            }
            val browser = runCatching {
                MediaBrowser(appContext, ComponentName(packageName, info.name), callback, null)
            }.getOrNull() ?: continue

            browsers[packageName] = browser
            runCatching { browser.connect() }.onFailure { error ->
                Log.w(LogTag, "Could not wake $packageName", error)
                browsers.remove(packageName)
            }
        }
    }

    fun release() {
        browsers.values.forEach { browser -> runCatching { browser.disconnect() } }
        browsers.clear()
    }

    private companion object {
        const val MediaBrowserServiceAction = "android.media.browse.MediaBrowserService"
    }
}
