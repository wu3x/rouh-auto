package org.wpfy.carlauncher.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import org.wpfy.carlauncher.R
import androidx.compose.ui.unit.sp

val ThmanyahFontFamily = FontFamily(
    Font(R.font.thmanyah_light, FontWeight.Light),
    Font(R.font.thmanyah_regular, FontWeight.Normal),
    Font(R.font.thmanyah_medium, FontWeight.Medium),
    Font(R.font.thmanyah_bold, FontWeight.Bold),
    Font(R.font.thmanyah_black, FontWeight.Black),
)

val CarTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineLarge = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineMedium = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleLarge = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontFamily = ThmanyahFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
)
