package org.wpfy.carlauncher.feature.settings

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

/**
 * Observes actual device metrics and visible package metadata. Does not modify packages,
 * clear data, stop processes, change system settings or request privileged permissions.
 */
internal object SystemHealthScanner {
    private fun size(bytes: Long): String = "%.2f GB".format(java.util.Locale.US, bytes / 1073741824.0)
    @Suppress("DEPRECATION")
    suspend fun scan(context: Context): String = withContext(Dispatchers.IO) {
        val out = StringBuilder()
        fun item(k: String, v: Any?) { out.appendLine("$k: ${v ?: "unavailable"}") }
        out.appendLine("ROUH Auto | System health and cleanup assessment (READ ONLY)")
        item("Scan time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
        item("Device", "${Build.MANUFACTURER} ${Build.MODEL} / ${Build.HARDWARE}")
        item("Android", "${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}")
        out.appendLine("=== RAM (actual ActivityManager readings) ===")
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val m = ActivityManager.MemoryInfo()
            am.getMemoryInfo(m)
            item("Total RAM", size(m.totalMem))
            item("Available RAM", size(m.availMem))
            item("Estimated used RAM", size((m.totalMem - m.availMem).coerceAtLeast(0)))
            item("Low-memory condition", m.lowMemory)
            item("Low-memory threshold", size(m.threshold))
            item("App memory class", "${am.memoryClass} MB")
            item("Large memory class", "${am.largeMemoryClass} MB")
        }.onFailure { item("RAM", "Unavailable: ${it.javaClass.simpleName}") }
        out.appendLine("=== Storage (actual StatFs readings) ===")
        listOf("Internal data" to Environment.getDataDirectory(), "System" to Environment.getRootDirectory()).forEach { (name, dir) ->
            runCatching {
                val stat = StatFs(dir.path)
                item("$name total", size(stat.totalBytes))
                item("$name free", size(stat.availableBytes))
                item("$name used estimate", size((stat.totalBytes - stat.freeBytes).coerceAtLeast(0)))
            }.onFailure { item(name, "Unavailable: ${it.javaClass.simpleName}") }
        }
        out.appendLine("=== Battery / power ===")
        runCatching {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (battery == null) item("Battery", "Not exposed by head unit")
            else {
                val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                item("Battery level", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "unavailable")
                item("Battery status code", battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
                item("Battery temperature", battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).let { if (it >= 0) "${it / 10.0} C" else "unavailable" })
            }
        }.onFailure { item("Battery", "Unavailable") }
        out.appendLine("=== Visible installed applications ===")
        val pm = context.packageManager
        val packages = runCatching { pm.getInstalledPackages(0) }.getOrElse { emptyList() }
        item("Visible packages", packages.size)
        item("System packages", packages.count { it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0 })
        item("User packages", packages.count { it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) == 0 })
        out.appendLine("Android package visibility can hide apps; counts are NOT guaranteed complete.")
        out.appendLine("=== Vendor and safety-critical packages (NEVER auto-disable) ===")
        val protected = listOf("com.hcn.", "com.autochips.", "com.android.bluetooth", "com.android.systemui", "com.android.settings", "com.mediatek.")
        packages.filter { pkg -> protected.any { pkg.packageName.startsWith(it) } }.sortedBy { it.packageName }.take(100).forEach {
            item(it.packageName, "version=${it.versionName}; system=${it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0}")
        }
        out.appendLine("=== Other visible user-installed applications (review candidates ONLY) ===")
        packages.filter { it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
            .sortedBy { it.packageName }.take(100).forEach { item("App", it.packageName) }
        out.appendLine("=== Assessment and limits ===")
        out.appendLine("This is a genuine read-only scan, not a cleaner. No apps were stopped, disabled, uninstalled or cleared.")
        out.appendLine("Android restricts per-app RAM, CPU, cache size and background process inspection; unavailable metrics are not fabricated.")
        out.appendLine("Do not disable HCN, MCU, CANBUS, Bluetooth, audio source, radio or Android system packages.")
        out.appendLine("Review the report before any future cleanup operation; no cleanup action is implemented.")
        out.toString()
    }
}

@Composable
internal fun SystemHealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CarHeader(title = "فحص النظام وتنظيفه")
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("فحص شامل للقراءة فقط: يقيس RAM والمساحة الفعلية وحالة الطاقة، ويعرض تطبيقات المصنع والتطبيقات المرئية. لا يحذف أو يعطّل أي شيء.")
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    report = runCatching { SystemHealthScanner.scan(context) }
                        .getOrElse { "فشل الفحص: ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
                    busy = false
                }
            }) { Text(if (busy) "جارٍ فحص الجهاز..." else "بدء فحص النظام الشامل") }
            if (report.isNotBlank()) {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ROUH system health report", report))
                    android.widget.Toast.makeText(context, "تم نسخ تقرير النظام", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("نسخ تقرير الفحص") }
                Text(report)
            }
            Text("التنظيف الفعلي غير مفعّل حفاظًا على خدمات MCU والبلوتوث وCAN. نراجع التقرير أولًا.")
            Button(onClick = onBack) { Text("رجوع") }
        }
    }
}
