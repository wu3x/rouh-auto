package org.wpfy.carlauncher.feature.map

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * UI groundwork only. Do not pretend to download maps until a licensed, hosted Saudi
 * offline-map catalog and a verified reader have been integrated.
 */
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
    val areas = listOf(
        "الرياض", "مكة المكرمة", "جدة", "المدينة المنورة", "الدمام والخبر",
        "الطائف", "أبها وخميس مشيط", "جازان", "تبوك", "حائل",
        "بريدة", "نجران", "الباحة", "عرعر", "سكاكا"
    )
    var selected by remember { mutableStateOf("جدة") }
    var showAreas by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var downloadState by remember { mutableStateOf("لم يبدأ التنزيل") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            downloadState = "جارٍ استيراد الخريطة..."
            scope.launch {
                downloadState = try { OfflineMbtiles.install(context, uri) }
                    catch (e: Exception) { "فشل الاستيراد: " + (e.message ?: "خطأ غير معروف") }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خرائط السعودية دون اتصال") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اختر المدينة التي ترغب في حفظ خريطتها:")
                TextButton(onClick = { showAreas = !showAreas }) { Text("$selected ▾") }
                if (showAreas) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(areas.size) { index ->
                            TextButton(onClick = {
                                selected = areas[index]
                                showAreas = false
                            }) { Text(areas[index]) }
                        }
                    }
                }
                Text(downloadState)
                Text("يمكنك استيراد خريطة جازان MBTiles من ملفات الجهاز. تعمل دون شبكة إذا كانت منطقتك داخل الخريطة.")
                Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("استيراد ملف MBTiles لجازان") }
                if (OfflineMbtiles.installed(context)) Text("✓ خريطة جازان مثبتة")
                Text("تنزيل بقية المدن يتطلب نشر حزمها أولًا.")
                if (selected == "جدة") {
                    Button(onClick = {
                        downloadState = "جارٍ فحص توفر خريطة جدة..."
                        SaudiMapPackageDownloader.downloadJeddah(context) { status ->
                            downloadState = status
                        }
                    }) { Text("تنزيل خريطة جدة") }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("حسنًا") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}
