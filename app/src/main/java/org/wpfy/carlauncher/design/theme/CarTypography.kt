package org.wpfy.carlauncher.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Android system sans-serif: platform-provided, Arabic-capable, no bundled proprietary fonts. */
val RouhSystemFontFamily = FontFamily.SansSerif

val CarTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineLarge = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineMedium = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleLarge = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontFamily = RouhSystemFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
)
