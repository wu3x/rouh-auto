package org.wpfy.carlauncher.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkCarColorScheme = darkColorScheme(
    primary = CarColors.Accent,
    onPrimary = CarColors.Background,
    primaryContainer = CarColors.AccentStrong,
    onPrimaryContainer = CarColors.TextPrimary,
    background = CarColors.Background,
    onBackground = CarColors.TextPrimary,
    surface = CarColors.Surface,
    onSurface = CarColors.TextPrimary,
    surfaceVariant = CarColors.SurfaceContainer,
    onSurfaceVariant = CarColors.TextSecondary,
    outline = CarColors.Outline,
)

@Composable
fun CarTheme(
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * textScale,
        ),
    ) {
        MaterialTheme(
            colorScheme = DarkCarColorScheme,
            typography = CarTypography,
            shapes = CarShapes,
            content = content,
        )
    }
}
