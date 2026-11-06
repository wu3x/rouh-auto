package org.wpfy.carlauncher.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun VehicleDiscoveryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<VehicleReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Text("اكتشاف اتصال السيارة", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("فحص آمن للقراءة فقط: نبحث عن خدمات السيارة وواجهات USB والبلوتوث المتاحة، بدون إرسال أوامر للسيارة.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scanning = true
                    error = null
                    report = null
                    scope.launch {
                        runCatching { discoverVehicle(context) }
                            .onSuccess { report = it }
                            .onFailure { error = "ما قدرنا نكمل الفحص: ${it.localizedMessage}" }
                        scanning = false
                    }
                }, enabled = !scanning) { Text(if (scanning) "اصبر شوي، ندور على اتصال السيارة..." else "ابدأ اكتشاف الاتصال") }
                if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                report?.let {
                    Text("خلصنا الفحص • ${it.candidateCount} مؤشرات محتملة", style = MaterialTheme.typography.titleMedium)
                    Text("المؤشرات ليست اتصالًا مؤكدًا. ما نعرض عدادات أو أعطال إلا بعد وصول بيانات حقيقية وفهم مصدرها.")
                }
            }
            items(report?.findings.orEmpty()) { finding ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(finding.group, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(finding.title, style = MaterialTheme.typography.titleMedium)
                        Text(finding.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (report != null) {
                item {
                    HorizontalDivider()
                    Text("بيئة استقبال البيانات", style = MaterialTheme.typography.titleMedium)
                    Text("CANBUS: بانتظار مصدر بيانات موثوق • OBD-II: بانتظار اتصال متوافق • قراءات المحرك والأبواب: غير متاحة حتى نتأكد من المصدر. ما فيه قيم تجريبية أو وهمية.")
                }
            }
        }
    }
}
