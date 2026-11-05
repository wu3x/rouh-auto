package org.wpfy.carlauncher.data.settings

import androidx.annotation.StringRes
import org.wpfy.carlauncher.R

enum class StartupScreen(val label: String, @StringRes val labelRes: Int) {
    Home("Home", R.string.startup_home),
    Apps("Apps", R.string.startup_apps),
}

enum class TextSizePreset(
    val label: String,
    @StringRes val labelRes: Int,
    val scale: Float,
) {
    Small("Small", R.string.text_size_small, 0.9f),
    Standard("Standard", R.string.text_size_standard, 1f),
    Large("Large", R.string.text_size_large, 1.15f),
}

enum class RailPosition(val label: String, @StringRes val labelRes: Int) {
    Left("Left", R.string.rail_left),
    Right("Right", R.string.rail_right),
    Bottom("Bottom", R.string.rail_bottom),
}

data class LauncherSettings(
    val onboardingCompleted: Boolean = false,
    val startOnBoot: Boolean = false,
    val startupScreen: StartupScreen = StartupScreen.Home,
    val textSizePreset: TextSizePreset = TextSizePreset.Standard,
    val preferredMediaAppKey: String? = null,
    val navigationAppKey: String? = null,
    val navigationCompatibilityMode: Boolean = false,
    val railPosition: RailPosition = RailPosition.Left,
)
