package org.wpfy.carlauncher.feature.map

import android.content.Context
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import kotlinx.coroutines.flow.MutableStateFlow

internal object MapAppearance {
    enum class Theme(val title: String, val detail: String) {
        CURRENT("الوضع الحالي", "الخريطة الأصلية بدون فلتر"),
        CLASSIC("Classic", "ألوان OpenStreetMap الطبيعية"),
        MIDNIGHT("Midnight", "خريطة داكنة"),
        MINIMAL("Minimal", "ألوان هادئة"),
        THREE_D("3D", "مظهر مجسم مستوحى من 3D وليس مباني حقيقية"),
        AUTO("تلقائي", "Classic نهارًا وMidnight ليلًا")
    }
    val selection = MutableStateFlow(Theme.CURRENT)
    fun initialize(context: Context) {
        val name = context.applicationContext.getSharedPreferences("rouh_map_appearance", Context.MODE_PRIVATE).getString("theme", Theme.CURRENT.name)
        selection.value = Theme.entries.firstOrNull { it.name == name } ?: Theme.CURRENT
    }
    fun set(context: Context, theme: Theme) {
        context.applicationContext.getSharedPreferences("rouh_map_appearance", Context.MODE_PRIVATE).edit().putString("theme", theme.name).apply()
        selection.value = theme
    }
    fun effective(theme: Theme, hour: Int): Theme =
        if (theme == Theme.AUTO) if (hour >= 18 || hour < 6) Theme.MIDNIGHT else Theme.CLASSIC else theme

    fun filter(theme: Theme): ColorFilter? {
        val matrix = when (theme) {
            Theme.CURRENT, Theme.CLASSIC, Theme.AUTO -> return null
            Theme.MIDNIGHT -> floatArrayOf(.19f,.14f,.09f,0f,8f, .10f,.24f,.10f,0f,12f, .13f,.19f,.27f,0f,23f, 0f,0f,0f,1f,0f)
            Theme.MINIMAL -> floatArrayOf(.27f,.53f,.16f,0f,22f, .27f,.53f,.16f,0f,25f, .27f,.53f,.16f,0f,30f, 0f,0f,0f,1f,0f)
            Theme.THREE_D -> floatArrayOf(.93f,.03f,.02f,0f,-8f, .02f,1.02f,.02f,0f,-5f, .02f,.04f,1.10f,0f,1f, 0f,0f,0f,1f,0f)
        }
        return ColorFilter.colorMatrix(ColorMatrix(matrix))
    }
}
