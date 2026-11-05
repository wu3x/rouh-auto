package org.wpfy.carlauncher.launcher

import android.content.ComponentName

data class LauncherApp(
    val component: ComponentName,
    val label: String,
) {
    val stableKey: String = component.flattenToShortString()
    val packageName: String = component.packageName
}
