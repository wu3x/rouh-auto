package org.wpfy.carlauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.component.CarIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.graphics.Color
import org.wpfy.carlauncher.feature.map.ArabicMapLabels

@Composable
internal fun MapNamesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy by ArabicMapLabels.downloadBusy.collectAsState()
    val progress by ArabicMapLabels.downloadProgress.collectAsState()
    val status by ArabicMapLabels.downloadStatus.collectAsState()
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CarHeader(title = "أسماء الأماكن في الخريطة", leading = { CarIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", onClick = onBack) })
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("حمّل أسماء الأماكن والشوارع للمناطق اللي سبق وحمّلت خريطتها. كل ما تزور منطقة جديدة وتتخزن خرائطها، ارجع هنا واضغط تحميل. تبقى الأسماء محفوظة للاستخدام بدون إنترنت.")
            Button(enabled = !busy, onClick = {
                scope.launch { ArabicMapLabels.downloadForCachedMap(context, null, null) }
            }) { Text(if (busy) "جارٍ تحميل الأسماء..." else "تحميل أسماء المناطق المحفوظة") }
            if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            androidx.compose.material3.Surface(color = Color(0xFF1D2938), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Text(if (busy) "جارٍ التحميل: ${(progress * 100).toInt()}% — $status" else "نتيجة التحميل: $status", modifier = Modifier.fillMaxWidth().padding(14.dp), color = Color.White)
            }
            Text("يحتاج إنترنت وقت التحميل. التحميل على دفعات لحماية أداء الشاشة واحترام حدود خوادم الخرائط.")
            Button(onClick = onBack) { Text("رجوع") }
        }
    }
}
