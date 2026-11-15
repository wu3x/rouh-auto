package org.wpfy.carlauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import org.wpfy.carlauncher.design.component.CarIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.theme.CarColors

@Composable
internal fun UpdatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<RouhRelease?>(null) }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("جارٍ التحقق من التحديثات...") }
    val installed = remember(context) { RouhUpdater.installedCode(context) }
    val installedName = remember(context) { RouhUpdater.installedName(context) }

    fun check() {
        if (busy) return
        busy = true
        checked = false
        downloading = false
        message = "جارٍ التحقق من GitHub..."
        scope.launch {
            try {
                release = RouhUpdater.check()
                checked = true
                message = if (release!!.versionCode > installed)
                    "نعم، لقينا تحديث جديد! تقدر تحمّله وتثبته من الزر اللي تحت." else "تم الفحص: أنت تستخدم أحدث إصدار متوفر."
            } catch (e: Exception) {
                release = null
                message = e.message ?: "تعذر التحقق. تأكد من اتصال الإنترنت."
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(Unit) { check() }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 1032.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CarHeader(title = "التحديثات", leading = { CarIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", onClick = onBack) })
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("الإصدار المثبت: $installedName (بناء $installed)", color = CarColors.TextSecondary)
            release?.let {
                Text("آخر إصدار: ${it.versionName} (بناء ${it.versionCode})", color = CarColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            }
            Surface(color = Color(0xFF1D2938), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (busy && !downloading) "جارٍ البحث…" else if (checked && release != null && release!!.versionCode > installed) "تحديث متوفر" else if (checked) "النسخة محدثة" else "نتيجة التحقق", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(message, color = Color(0xFFE0E8F2))
                }
            }
            if (downloading) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                Text("التنزيل: $progress%", color = Color.White)
            }
            Button(enabled = !busy, onClick = { check() }) { Text("التحقق من التحديثات") }
            val available = release
            if (available != null && available.versionCode > installed) {
                Button(enabled = !busy, onClick = {
                    busy = true
                    downloading = true
                    progress = 0
                    message = "جارٍ تنزيل التحديث والتحقق من سلامته..."
                    scope.launch {
                        try {
                            val apk = RouhUpdater.download(context, available) { progress = it }
                            message = "اكتمل التنزيل. وافق على التثبيت من نافذة أندرويد."
                            RouhUpdater.install(context, apk)
                        } catch (e: Exception) {
                            message = e.message ?: "تعذر تنزيل أو تثبيت التحديث"
                        } finally {
                            busy = false
                            downloading = false
                        }
                    }
                }) { Text("تحميل وتثبيت التحديث") }
            }
            Text(
                "يحتاج الإنترنت للتحقق والتنزيل. أندرويد يطلب موافقتك على التثبيت، وقد يطلب تفعيل السماح بالتثبيت من هذا المصدر أول مرة.",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )

        }
    }
}
