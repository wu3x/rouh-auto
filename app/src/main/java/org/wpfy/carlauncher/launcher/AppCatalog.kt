package org.wpfy.carlauncher.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import java.text.Collator

class AppCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(64)

    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val collator = Collator.getInstance()
        resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != appContext.packageName }
            .filter { it.activityInfo.enabled && it.activityInfo.applicationInfo.enabled }
            .map { resolveInfo ->
                LauncherApp(
                    component = android.content.ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name,
                    ),
                    label = resolveInfo.loadLabel(packageManager).toString().ifBlank {
                        resolveInfo.activityInfo.packageName
                    },
                )
            }
            .distinctBy(LauncherApp::stableKey)
            .sortedWith { left, right -> collator.compare(left.label, right.label) }
            .toList()
    }

    suspend fun loadIcon(app: LauncherApp): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache.get(app.stableKey)?.let { return@withContext it }

        runCatching {
            packageManager
                .getActivityIcon(app.component)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
                .also { iconCache.put(app.stableKey, it) }
        }.getOrNull()
    }

    fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                iconCache.evictAll()
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }

        awaitClose {
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }.conflate()
}
