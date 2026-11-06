package org.wpfy.carlauncher.feature.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private data class TestMetrics(
    val ramFree: Long, val ramTotal: Long, val storageFree: Long, val storageTotal: Long,
    val operationMs: Double? = null, val writeMBps: Double? = null, val readMBps: Double? = null
)

private fun snapshot(context: Context): TestMetrics {
    val memory = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    val storage = StatFs(Environment.getDataDirectory().path)
    return TestMetrics(memory.availMem, memory.totalMem, storage.availableBytes, storage.totalBytes)
}

private fun gb(bytes: Long): String = "%.1f".format(java.util.Locale.US, bytes / 1073741824.0)

private suspend fun runSystemTest(context: Context): TestMetrics = withContext(Dispatchers.IO) {
    val before = snapshot(context)
    val start = SystemClock.elapsedRealtimeNanos()
    var check = 1L
    repeat(100_000) { check = (check * 31L + it) xor (check ushr 9) }
    val operationMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
    // A bounded 4 MiB write/read in app cache, not a whole-device disk speed claim.
    val testFile = File.createTempFile("launcher-benchmark-", ".bin", context.cacheDir)
    var writeSpeed: Double? = null
    var readSpeed: Double? = null
    try {
        val block = ByteArray(64 * 1024) { (it % 251).toByte() }
        val writeStart = SystemClock.elapsedRealtimeNanos()
        testFile.outputStream().buffered().use { output ->
            repeat(64) { output.write(block) }
            output.flush()
        }
        val writeSeconds = (SystemClock.elapsedRealtimeNanos() - writeStart) / 1e9
        writeSpeed = 4.0 / writeSeconds.coerceAtLeast(0.001)
        val readStart = SystemClock.elapsedRealtimeNanos()
        testFile.inputStream().buffered().use { input ->
            val buf = ByteArray(64 * 1024)
            while (input.read(buf) != -1) Unit
        }
        val readSeconds = (SystemClock.elapsedRealtimeNanos() - readStart) / 1e9
        readSpeed = 4.0 / readSeconds.coerceAtLeast(0.001)
    } finally {
        testFile.delete()
    }
    before.copy(operationMs = operationMs, writeMBps = writeSpeed, readMBps = readSpeed)
}

@Composable
internal fun SystemTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var metrics by remember { mutableStateOf(snapshot(context)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val navy = Color(0xFF10152B)
    val tile = Color(0xFF202849)
    Column(Modifier.fillMaxSize().background(navy).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Text("اختبار النظام", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item {
                Text("تقرير أداء الشاشة", color = Color(0xFFAAB8E0), style = MaterialTheme.typography.titleMedium)
                Text("الذاكرة والتخزين قراءات فعلية. سرعة الاختبار تخص التطبيق فقط.", color = Color(0xFFB6BFDA), style = MaterialTheme.typography.bodySmall)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("الذاكرة المتاحة", gb(metrics.ramFree), "GB من ${gb(metrics.ramTotal)}", tile, Modifier.weight(1f))
                    MetricTile("التخزين المتاح", gb(metrics.storageFree), "GB من ${gb(metrics.storageTotal)}", tile, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("اختبار المعالج", metrics.operationMs?.let { "%.1f".format(java.util.Locale.US, it) } ?: "—", "ms • عملية حسابية داخل التطبيق", tile, Modifier.weight(1f))
                    MetricTile("سرعة الكتابة", metrics.writeMBps?.let { "%.1f".format(java.util.Locale.US, it) } ?: "—", "MB/s • اختبار 4 MB", tile, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("سرعة القراءة", metrics.readMBps?.let { "%.1f".format(java.util.Locale.US, it) } ?: "—", "MB/s • قد تتأثر بذاكرة التخزين المؤقت", tile, Modifier.weight(1f))
                    MetricTile("استجابة الواجهة", "—", "غير مقاسة بعد • ما نعرض قيمة تقديرية", tile, Modifier.weight(1f))
                }
            }
            item {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = Color(0xFFFFA5A5)) }
                Button(enabled = !busy, onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { runSystemTest(context) }
                            .onSuccess { metrics = it }
                            .onFailure { error = "ما قدرنا نكمل الاختبار: ${it.localizedMessage}" }
                        busy = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "اصبر شوي، نقيس أداء الشاشة..." else if (metrics.operationMs == null) "بدء اختبار النظام" else "إعادة الاختبار")
                }
                Text("لا تختبر أثناء القيادة. هذه قياسات محلية تقريبية وتتغير حسب حمل النظام، ولا تمثل أداء السيارة أو كمبيوترها.", color = Color(0xFF9EAACD), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(color, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = Color(0xFFC1C9E6), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color(0xFF82D9CB), style = MaterialTheme.typography.headlineMedium)
        Text(unit, color = Color(0xFFA7B3D6), style = MaterialTheme.typography.bodySmall)
    }
}
