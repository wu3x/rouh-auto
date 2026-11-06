package org.wpfy.carlauncher.feature.settings

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import org.wpfy.carlauncher.design.component.CarHeader

private data class FoundFile(val uri: Uri, val name: String, val size: Long, val category: String)
private data class ScanResult(val files: List<FoundFile>, val scanned: Int, val truncated: Boolean)

@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("maintenance", Context.MODE_PRIVATE) }
    var folder by remember { mutableStateOf(prefs.getString("folder", null)?.let(Uri::parse)) }
    var busy by remember { mutableStateOf(false) }
    var scanFinished by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<FoundFile?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        if (response.resultCode == Activity.RESULT_OK) {
            response.data?.data?.let { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                folder = uri
                prefs.edit().putString("folder", uri.toString()).apply()
                result = null
                error = null
            }
        }
    }
    val storage = remember(refresh) {
        val stat = StatFs(Environment.getDataDirectory().path)
        (stat.totalBytes - stat.availableBytes) to stat.availableBytes
    }
    val ram = remember(refresh) {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        info
    }
    fun scan() {
        val selected = folder ?: return
        busy = true
        scanFinished = false
        error = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { scanFolder(context, selected) } }
                .onSuccess { result = it; scanFinished = true }
                .onFailure { error = "ما قدرنا نفحص المجلد: ${it.localizedMessage}" }
            busy = false
            refresh++
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Text("صيانة النظام", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("التخزين", style = MaterialTheme.typography.titleLarge)
                Text("المستخدم: ${formatSize(storage.first)} • المتاح: ${formatSize(storage.second)}")
                LinearProgressIndicator(
                    progress = { storage.first.toFloat() / (storage.first + storage.second).coerceAtLeast(1L) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("الأداء", style = MaterialTheme.typography.titleLarge)
                Text("الذاكرة المتاحة: ${formatSize(ram.availMem)} من ${formatSize(ram.totalMem)}")
                Text("هذه قراءة فعلية للذاكرة والتخزين، وليست أداة لتسريع المعالج.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { refresh++ }) { Text("تحديث القياسات") }
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                    }) { Text("إعدادات التخزين") }
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
                    }) { Text("إدارة التطبيقات") }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("فحص الملفات", style = MaterialTheme.typography.titleLarge)
                Text("اختر مجلدًا تسمح بفحصه. يعرض ملفات APK القديمة والمكررات المؤكدة بمحتوى متطابق. لا يُحذف شيء دون موافقتك.")
                Text("أندرويد 13 قد يمنع اختيار جذر التخزين أو مجلد Downloads كاملًا؛ اختر مجلدًا فرعيًا متاحًا.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        })
                    }) { Text("السماح بالوصول لمجلد") }
                    Button(onClick = { scan() }, enabled = folder != null && !busy) {
                        Text(if (busy) "اصبر شوي، نفحص لك..." else "فحص الآن")
                    }
                }
                if (folder != null) Text("المجلد المحدد: ${folder?.lastPathSegment.orEmpty()}")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                result?.let {
                    Text(if (it.files.isEmpty()) "أمورك طيبة، ما لقينا ملفات تحتاج مراجعة" else "خلصنا الفحص، وهذي الملفات اللي تستاهل تراجعها")
                    Text("تم فحص ${it.scanned} ملف • النتائج: ${it.files.size}")
                    if (it.truncated) Text("تم إيقاف الفحص عند 3000 ملف لحماية أداء الشاشة.")
                }
            }
            items(result?.files.orEmpty(), key = { it.uri.toString() }) { file ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name)
                        Text("${file.category} • ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { pendingDelete = file }) { Text("حذف") }
                }
                HorizontalDivider()
            }
        }
    }
    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف ${file.name} نهائيًا؟ تأكد أنه ليس ملفًا تحتاجه.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val deleted = withContext(Dispatchers.IO) {
                            runCatching { DocumentsContract.deleteDocument(context.contentResolver, file.uri) }.getOrDefault(false)
                        }
                        if (deleted) result = result?.copy(files = result!!.files.filterNot { it.uri == file.uri })
                        else error = "ما قدرنا نحذف الملف، تأكد من صلاحية المجلد."
                        pendingDelete = null
                        refresh++
                    }
                }) { Text("حذف الملف") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") } }
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "${bytes / 1024} KB"
}

private fun scanFolder(context: Context, tree: Uri): ScanResult {
    val resolver = context.contentResolver
    val root = DocumentsContract.getTreeDocumentId(tree)
    val queue = ArrayDeque<String>()
    queue.add(root)
    val candidates = mutableListOf<FoundFile>()
    var count = 0
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE
    )
    while (queue.isNotEmpty() && count < 3000) {
        val parent = queue.removeFirst()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        resolver.query(children, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(projection[0])
            val nameCol = cursor.getColumnIndexOrThrow(projection[1])
            val typeCol = cursor.getColumnIndexOrThrow(projection[2])
            val sizeCol = cursor.getColumnIndexOrThrow(projection[3])
            while (cursor.moveToNext() && count < 3000) {
                val id = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val mime = cursor.getString(typeCol)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    queue.add(id)
                } else {
                    count++
                    val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                    candidates.add(FoundFile(DocumentsContract.buildDocumentUriUsingTree(tree, id), name, size, ""))
                }
            }
        }
    }
    val apks = candidates.filter { it.name.endsWith(".apk", ignoreCase = true) }
        .map { it.copy(category = "ملف تثبيت APK — تحقق قبل الحذف") }
    val duplicates = mutableListOf<FoundFile>()
    val groups = candidates.filter { it.size > 0L && it.size <= 250L * 1024 * 1024 }.groupBy { it.size }
    for (group in groups.values) {
        if (group.size < 2) continue
        val hashes = mutableMapOf<String, MutableList<FoundFile>>()
        for (file in group) {
            val digest = runCatching {
                val md = MessageDigest.getInstance("SHA-256")
                resolver.openInputStream(file.uri)?.use { stream ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        md.update(buffer, 0, n)
                    }
                    md.digest().joinToString("") { "%02x".format(it) }
                }
            }.getOrNull()
            if (digest != null) hashes.getOrPut(digest) { mutableListOf() }.add(file)
        }
        hashes.values.filter { it.size > 1 }.forEach { matches ->
            duplicates.addAll(matches.drop(1).map { it.copy(category = "نسخة مكررة — تطابق SHA-256") })
        }
    }
    return ScanResult((apks + duplicates).distinctBy { it.uri }, count, count >= 3000)
}
