package org.wpfy.carlauncher.feature.map

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

object MapDownloadPreferences {
    private const val PREFS = "rouh_map_download"
    private const val HIDE_PROMPT = "hide_prompt"

    fun isPromptHidden(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(HIDE_PROMPT, false)

    fun setPromptHidden(context: Context, hidden: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(HIDE_PROMPT, hidden).apply()
    }
}

@Composable
fun MapDownloadDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            status = "جارٍ استيراد الخريطة..."
            scope.launch {
                try {
                    status = OfflineMbtiles.install(context, uri)
                    MapZoomState.updateZoomLevel(14f)
                } catch (e: Exception) {
                    status = "تعذر الاستيراد: ${e.message ?: "خطأ غير معروف"}"
                } finally {
                    busy = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("خرائط دون اتصال") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("جازان")
                Text(if (OfflineMbtiles.installed(context)) "الخريطة مثبتة على الجهاز" else "خريطة جاهزة للتنزيل (20 MB تقريبًا)")
                if (status.isNotEmpty()) Text(status)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = {
                        busy = true
                        status = "جارٍ الاتصال بالخادم..."
                        scope.launch {
                            try {
                                status = JazanMapDownloader.download(context) { status = it }
                            } catch (e: Exception) {
                                status = "تعذر التنزيل: ${e.message ?: "خطأ غير معروف"}"
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text(if (busy) "جارٍ المعالجة..." else if (OfflineMbtiles.installed(context)) "إعادة تنزيل خريطة جازان" else "تنزيل خريطة جازان")
                }
                TextButton(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) {
                    Text("استيراد ملف MBTiles من الجهاز")
                }
                Text("تعمل الخريطة دون إنترنت بعد تثبيتها. يلزم دعم GPS في الجهاز لتتبع الموقع.")
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("إغلاق") }
        },
    )
}
