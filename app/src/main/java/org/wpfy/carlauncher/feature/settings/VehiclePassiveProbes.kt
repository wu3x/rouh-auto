package org.wpfy.carlauncher.feature.settings

import android.content.Context
import android.os.Build
import java.io.File

/** Metadata probes only: no device nodes are opened, no commands are transmitted. */
internal fun passiveVehicleProbes(context: Context): List<VehicleFinding> {
    val pm = context.packageManager
    val result = mutableListOf<VehicleFinding>()
    result.add(VehicleFinding("النظام", "معلومات الشاشة", "المصنّع: ${Build.MANUFACTURER} • الطراز: ${Build.MODEL} • اللوحة: ${Build.BOARD} • الجهاز: ${Build.DEVICE}"))
    listOf(
        "android.hardware.type.automotive" to "نظام Android Automotive",
        "android.hardware.usb.host" to "USB Host",
        "android.hardware.bluetooth" to "Bluetooth",
        "android.hardware.location.gps" to "GPS"
    ).forEach { (feature, label) ->
        result.add(VehicleFinding("قدرات النظام", label, if (pm.hasSystemFeature(feature)) "النظام يعلن توفرها" else "النظام ما يعلن توفرها"))
    }
    listOf("/dev/ttyS0", "/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS3",
        "/dev/ttyMT0", "/dev/ttyMT1", "/dev/ttyUSB0", "/dev/ttyACM0",
        "/dev/can0", "/sys/class/net/can0").forEach { path ->
        val node = File(path)
        if (node.exists()) {
            result.add(VehicleFinding("منفذ محتمل", path, "موجود • إذن القراءة: ${if (node.canRead()) "متاح" else "مقيد"} • ما فتحنا المنفذ"))
        }
    }
    result.add(VehicleFinding("BCM", "بيانات الأبواب والهيكل", "ما نقدر نؤكد توفرها إلا بعد التعرف على مصدر CAN وواجهة القراءة الفعلية"))
    result.add(VehicleFinding("التوافق", "إلنترا — صنع 2016", "ملف تعريف مبدئي، وليس إثباتًا لنوع وحدة BCM أو بروتوكول CAN"))
    return result
}
