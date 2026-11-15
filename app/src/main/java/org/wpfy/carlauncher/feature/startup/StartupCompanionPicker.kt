package org.wpfy.carlauncher.feature.startup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun StartupCompanionPicker(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(StartupCompanion.selected(context)) }
    var search by remember { mutableStateOf("") }
    val apps = remember(context) { StartupCompanion.installedApps(context) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
        Text("تشغيل تطبيق أصلي عند بدء اللانشر", color = Color.White)
        Text("اختياري. يشمل تطبيقات النظام الظاهرة لأندرويد. لا نرسل أمر تشغيل موسيقى، لكن بعض تطبيقات الشركة قد تشغّل الصوت بنفسها.", color = Color(0xFFCDD7E4))
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("ابحث باسم التطبيق أو الحزمة") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { StartupCompanion.set(context, null); selected = null }) { Text("تعطيل") }
            Button(onClick = onBack) { Text("رجوع") }
        }
        Text(if (selected == null) "غير مفعّل" else "المحدد: $selected", color = Color.White)
        LazyColumn {
            items(apps.filter { (name, pkg) -> name.contains(search, true) || pkg.contains(search, true) }, key = { it.second }) { (name, pkg) ->
                Column(modifier = Modifier.fillMaxWidth().clickable {
                    StartupCompanion.set(context, pkg)
                    selected = pkg
                }.padding(vertical = 8.dp)) {
                    Text((if (pkg == selected) "✓ " else "") + name, color = Color.White)
                    Text(pkg, color = Color(0xFFB5C7DA))
                }
            }
        }
    }
}
