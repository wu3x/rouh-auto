package org.wpfy.carlauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.component.CarIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.graphics.Color
import org.wpfy.carlauncher.feature.map.MapAppearance

@Composable
internal fun MapAppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MapAppearance.initialize(context) }
    val selected by MapAppearance.selection.collectAsState()
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CarHeader(title = "مظهر الخريطة", leading = { CarIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", onClick = onBack) })
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("الثيمات مدمجة بالتطبيق، تعمل على نفس خرائط OpenStreetMap المحفوظة، وأسماء الأماكن تبقى فوق الخريطة في جميع الأوضاع.")
            MapAppearance.Theme.entries.forEach { theme ->
                Surface(color = Color(0xFF1D2938), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { MapAppearance.set(context, theme) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        RadioButton(selected = selected == theme, onClick = { MapAppearance.set(context, theme) })
                        Column {
                            Text(theme.title, color = Color.White)
                            Text(theme.detail, color = Color(0xFFCFD8E3))
                        }
                    }
                }
            }
            Text("تنبيه: 3D الحالي تأثير بصري فقط؛ المباني ثلاثية الأبعاد الحقيقية تحتاج خرائط Vector. الوضع التلقائي يعتمد مؤقتًا على ساعة الجهاز (6 صباحًا–6 مساءً)، وليس حساب الغروب الفلكي.")
            Button(onClick = onBack) { Text("رجوع") }
        }
    }
}
