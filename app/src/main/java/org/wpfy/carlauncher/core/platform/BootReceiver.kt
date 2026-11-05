package org.wpfy.carlauncher.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.wpfy.carlauncher.MainActivity
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settingsRepository = SettingsRepository(context)
                if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    runCatching {
                        settingsRepository.migrateExistingInstallOnUpgrade(defaultRailPosition(context))
                    }
                } else if (settingsRepository.current().startOnBoot) {
                    runCatching {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun defaultRailPosition(context: Context): RailPosition {
    val configuration = context.resources.configuration
    return defaultRailPositionForDisplay(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
}

internal fun defaultRailPositionForDisplay(
    screenWidthDp: Int,
    screenHeightDp: Int,
): RailPosition {
    val forcedLandscapeWidthDp = maxOf(screenWidthDp, screenHeightDp)
    return if (forcedLandscapeWidthDp < 880) {
        RailPosition.Bottom
    } else {
        RailPosition.Left
    }
}
