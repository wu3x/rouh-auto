package org.wpfy.carlauncher.feature.settings

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Passive inventory only. Never opens a CAN port or sends vehicle commands. */
internal data class VehicleFinding(val group: String, val title: String, val detail: String)
internal data class VehicleReport(val findings: List<VehicleFinding>) {
    val candidateCount: Int get() = findings.count { it.group == "احتمال اتصال" }
}

internal suspend fun discoverVehicle(context: Context): VehicleReport = withContext(Dispatchers.IO) {
    val results = mutableListOf<VehicleFinding>()
    val pm = context.packageManager
    val keywords = listOf("canbus", "canbox", "mcu", "obd", "vehicle", "carservice", "carinfo", "tbox", "syu.ms", "com.microntek")
    @Suppress("DEPRECATION")
    val packages = pm.getInstalledPackages(PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS)
    packages.forEach { pkg ->
        val name = pkg.packageName
        val services = pkg.services.orEmpty().map { it.name }
        val providers = pkg.providers.orEmpty().map { it.name }
        val match = !name.startsWith("com.google.") && !name.startsWith("com.gold.android.") && !name.startsWith("com.adups.") && (listOf(name) + services + providers).any { part -> keywords.any { key -> part.contains(key, ignoreCase = true) } }
        if (match && name != context.packageName) {
            results.add(VehicleFinding("احتمال اتصال", name, "تطبيق أو خدمة مرتبطة بالسيارة • خدمات: ${services.size} • مزودات: ${providers.size}. وجودها لا يثبت توفر بيانات CAN."))
        }
    }
    val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager
    results.add(VehicleFinding("USB", "الأجهزة المتصلة: ${usb.deviceList.size}", if (usb.deviceList.isEmpty()) "ما لقينا أجهزة USB ظاهرة للنظام" else "الأجهزة المعروضة لا تعني بالضرورة أنها CANBUS"))
    usb.deviceList.values.forEach { d ->
        results.add(VehicleFinding("USB", "VID ${d.vendorId} / PID ${d.productId}", "فئة الجهاز: ${d.deviceClass} • صلاحية الوصول: ${if (usb.hasPermission(d)) "متاحة" else "تحتاج موافقة"}"))
    }
    val bluetooth = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = bluetooth?.adapter
    val permitted = Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    results.add(VehicleFinding("Bluetooth", "حالة البلوتوث", when {
        adapter == null -> "البلوتوث غير متاح على الجهاز"
        !permitted -> "تحتاج إذن الأجهزة القريبة لعرض الأجهزة المقترنة"
        !adapter.isEnabled -> "البلوتوث مطفي"
        else -> "البلوتوث شغال"
    }))
    if (adapter != null && permitted && adapter.isEnabled) {
        runCatching { adapter.bondedDevices }.getOrNull()?.forEach { device ->
            val name = runCatching { device.name }.getOrNull().orEmpty()
            results.add(VehicleFinding(if (name.contains("OBD", true) || name.contains("ELM", true)) "احتمال اتصال" else "Bluetooth", name.ifBlank { "جهاز مقترن" }, "عنوان الجهاز: ${device.address} • مقترن فقط، لم يتم الاتصال"))
        }
    }
    if (results.none { it.group == "احتمال اتصال" }) {
        results.add(VehicleFinding("النتيجة", "ما لقينا اتصال سيارة مؤكد", "الفحص يكتشف الواجهات التي يسمح أندرويد برؤيتها. ممكن تكون وحدة CAN مخفية عن التطبيقات أو غير موصولة."))
    }
    results.addAll(passiveVehicleProbes(context))
    VehicleReport(results)
}
