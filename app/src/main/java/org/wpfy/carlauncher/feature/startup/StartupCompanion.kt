package org.wpfy.carlauncher.feature.startup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Opt-in vendor app warm-up. Never sends PLAY or media-key events. */
object StartupCompanion {
    private const val Prefs = "rouh_startup_companion"
    fun selected(context: Context): String? =
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString("package", null)
    fun set(context: Context, packageName: String?) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString("package", packageName).apply()
    }
    fun installedApps(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .map { it.loadLabel(pm).toString() to it.packageName }
            .sortedBy { it.first.lowercase() }
    }
    fun launch(activity: Activity) {
        val pkg = selected(activity) ?: return
        val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Log.w("RouhStartup", "Selected app has no public launch activity: $pkg")
            return
        }
        // Open once per Activity creation, then return HOME to restore the launcher.
        runCatching {
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Handler(Looper.getMainLooper()).postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
            }, 1500L)
        }.onFailure { Log.w("RouhStartup", "Cannot open $pkg", it) }
    }
}
