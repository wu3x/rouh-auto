package org.wpfy.carlauncher.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.component.CarIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.graphics.Color

/** Read-only discovery. No vendor services are started or bound. */
internal object BluetoothHeadUnitReport {
    @Suppress("DEPRECATION")
    suspend fun collect(context: Context): String = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val out = StringBuilder()
        out.appendLine("ROUH Auto - Bluetooth/MCU diagnostic report")
        out.appendLine("Device: ${Build.MANUFACTURER} / ${Build.BRAND} / ${Build.MODEL}")
        out.appendLine("Board: ${Build.BOARD}; hardware: ${Build.HARDWARE}; device: ${Build.DEVICE}")
        out.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        out.appendLine("Build display: ${Build.DISPLAY}")
        out.appendLine("App: ${context.packageName}")
        out.appendLine()
        val intents = listOf(
            "MediaBrowserService" to "android.media.browse.MediaBrowserService",
            "Bluetooth A2DP Sink" to "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED",
            "Bluetooth AVRCP Controller" to "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED",
            "Bluetooth A2DP" to "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
        )
        intents.forEach { (label, action) ->
            out.appendLine("=== $label ===")
            val matches = runCatching { pm.queryIntentServices(Intent(action), PackageManager.GET_META_DATA) }
                .getOrDefault(emptyList())
            if (matches.isEmpty()) out.appendLine("No visible matching service")
            matches.take(80).forEach { info ->
                val service = info.serviceInfo
                out.appendLine("Service: ${service?.packageName}/${service?.name}; exported=${service?.exported}; permission=${service?.permission ?: "none"}")
            }
        }
        out.appendLine("=== Visible Bluetooth / MCU / radio applications ===")
        val pattern = Regex("bluetooth|btmusic|btaudio|avrcp|a2dp|mcu|canbus|carservice|radio|zlink|tlink|phonelink|com\\.tw|com\\.syu|com\\.microntek|com\\.wits", RegexOption.IGNORE_CASE)
        val installed = runCatching { pm.getInstalledPackages(PackageManager.GET_SERVICES) }.getOrDefault(emptyList())
        var found = 0
        installed.forEach { pkg ->
            val label = runCatching { pm.getApplicationLabel(pkg.applicationInfo!!).toString() }.getOrDefault("")
            val services = pkg.services.orEmpty()
            val relevant = pattern.containsMatchIn(pkg.packageName) ||
                pattern.containsMatchIn(label) || services.any { pattern.containsMatchIn(it.name) }
            if (relevant && found++ < 100) {
                out.appendLine("App: $label | ${pkg.packageName} | version=${pkg.versionName}")
                services.take(40).forEach { service ->
                    out.appendLine("  service: ${service.name}; exported=${service.exported}; permission=${service.permission ?: "none"}")
                }
            }
        }
        if (found == 0) out.appendLine("No matching visible packages")
        out.appendLine()
        out.appendLine("=== Bluetooth system settings (non-sensitive) ===")
        out.appendLine("Bluetooth enabled: " + runCatching {
            Settings.Global.getInt(context.contentResolver, "bluetooth_on", -1)
        }.getOrDefault(-1))
        out.appendLine("=== Visibility note ===")
        out.appendLine("Android package visibility and vendor restrictions can hide system/MCU services. An empty result does not mean the service is absent.")
        out.appendLine("Report excludes paired device identifiers, MAC addresses, location and credentials.")
        out.toString()
    }
}

@Composable
internal fun BluetoothDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CarHeader(title = "تشخيص بلوتوث الشاشة", leading = { CarIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", onClick = onBack) })
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("الفحص للقراءة فقط. اختبار تشغيل الخدمة إجراء منفصل يدوي. جرّبه والسيارة واقفة.")
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    report = runCatching { BluetoothHeadUnitReport.collect(context) }
                        .getOrElse { "تعذر الفحص: ${it.message}" }
                    busy = false
                }
            }) { Text(if (busy) "جارٍ الفحص..." else "فحص خدمات الشركة المصنّعة وMCU") }

            Text("شغّل أغنية من الجوال والصوت ساكت، ثم جرّب تفعيل خدمة المصنع يدويًا.")
            Button(enabled = !busy, onClick = {
                val component = ComponentName("com.hcn.bluetoothservice", "com.hcn.bluetooth.service.BluetoothMusicService")
                testResult = runCatching {
                    val started = context.startService(Intent().setComponent(component))
                    if (started == null) "لم يقبل أندرويد بدء الخدمة (null)."
                    else "قبل أندرويد طلب بدء الخدمة: " + started.flattenToShortString() + ". هذا لا يؤكد تشغيل الصوت."
                }.getOrElse { "رفض بدء الخدمة: " + it.javaClass.simpleName + ": " + it.message.orEmpty().take(240) }
            }) { Text("اختبار تفعيل Bluetooth Music") }
            if (testResult.isNotBlank()) {
                Text(testResult)
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ROUH Bluetooth service test", testResult))
                    android.widget.Toast.makeText(context, "تم نسخ النتيجة", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("نسخ نتيجة الاختبار") }
            }
            if (report.isNotBlank()) {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ROUH Bluetooth report", report))
                    android.widget.Toast.makeText(context, "تم نسخ التقرير", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("نسخ التقرير") }
                Text(report)
            }
            Button(onClick = onBack) { Text("رجوع") }
        }
    }
}
