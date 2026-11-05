package org.wpfy.carlauncher.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AppLauncher(context: Context) {
    private val appContext = context.applicationContext

    fun launch(app: LauncherApp): Boolean = runCatching {
        appContext.startActivity(
            Intent.makeMainActivity(app.component).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess

    fun openAppInfo(app: LauncherApp): Boolean = runCatching {
        appContext.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
}
