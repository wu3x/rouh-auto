package org.wpfy.carlauncher.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

/**
 * Information for matching a head unit with compatible firmware.
 * Read-only; deliberately excludes serials, Android IDs, MAC addresses and user data.
 */
internal object FullDeviceReport {
    @Suppress("DEPRECATION")
    fun collect(context: Context): String {
        val pm = context.packageManager
        val out = StringBuilder()
        fun line(key: String, value: Any?) { out.appendLine("$key: ${value ?: "unavailable"}") }
        out.appendLine("ROUH Auto | Full head-unit hardware / firmware report")
        out.appendLine("=== Manufacturer and hardware ===")
        line("Manufacturer", Build.MANUFACTURER)
        line("Brand", Build.BRAND)
        line("Model", Build.MODEL)
        line("Product", Build.PRODUCT)
        line("Device", Build.DEVICE)
        line("Board", Build.BOARD)
        line("Hardware", Build.HARDWARE)
        line("Bootloader", Build.BOOTLOADER)
        line("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
        line("Supported 32-bit ABIs", Build.SUPPORTED_32_BIT_ABIS.joinToString(", "))
        line("Supported 64-bit ABIs", Build.SUPPORTED_64_BIT_ABIS.joinToString(", "))
        out.appendLine("=== Android / firmware identity ===")
        line("Android release (as reported)", Build.VERSION.RELEASE)
        line("Android SDK API", Build.VERSION.SDK_INT)
        line("Android codename", Build.VERSION.CODENAME)
        line("Android incremental", Build.VERSION.INCREMENTAL)
        line("Security patch", if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "unavailable")
        line("Build ID", Build.ID)
        line("Build display", Build.DISPLAY)
        line("Build fingerprint", Build.FINGERPRINT)
        line("Build type", Build.TYPE)
        line("Build tags", Build.TAGS)
        line("Kernel", runCatching { File("/proc/version").readText().trim().take(500) }.getOrElse { "unavailable" })
        out.appendLine("=== Screen and storage ===")
        runCatching {
            val metrics = DisplayMetrics()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            line("Display", "${metrics.widthPixels}x${metrics.heightPixels}, dpi=${metrics.densityDpi}")
        }.onFailure { line("Display", "unavailable") }
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            line("Data partition total bytes", stat.totalBytes)
            line("Data partition available bytes", stat.availableBytes)
        }.onFailure { line("Storage", "unavailable") }
        out.appendLine("=== Vendor / MCU / Bluetooth packages ===")
        val prefixes = listOf("com.hcn.", "com.autochips.", "com.mediatek.", "com.android.bluetooth")
        val packages = runCatching { pm.getInstalledPackages(0) }.getOrDefault(emptyList())
            .filter { pkg -> prefixes.any { pkg.packageName.startsWith(it) } }
            .sortedBy { it.packageName }
        if (packages.isEmpty()) out.appendLine("No matching visible packages")
        packages.take(100).forEach { pkg ->
            line(pkg.packageName, "versionName=${pkg.versionName ?: "unknown"}, versionCode=${if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode}")
        }
        out.appendLine("=== Important compatibility warning ===")
        out.appendLine("Android release string and SDK API can disagree on customized head units. Both are shown without correction.")
        out.appendLine("This report does NOT prove MCU firmware version, PCB revision, display controller, CANBUS model, or firmware compatibility.")
        out.appendLine("Verify exact MCU and board identifiers in factory settings before flashing any firmware.")
        out.appendLine("Privacy: serial number, Android ID, MAC, IMEI, paired devices and accounts are excluded.")
        return out.toString()
    }
}
