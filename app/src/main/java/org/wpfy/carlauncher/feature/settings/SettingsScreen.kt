package org.wpfy.carlauncher.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import org.wpfy.carlauncher.feature.home.WelcomePreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.data.persistence.MaxPinnedApps
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.data.settings.TextSizePreset
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.component.CarIconButton
import org.wpfy.carlauncher.design.component.CarListRow
import org.wpfy.carlauncher.design.component.CarSwitch
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.launcher.LauncherApp

@Composable
fun SettingsRoute(stateHolder: SettingsStateHolder) {
    val context = LocalContext.current
    val diagnostics = remember(context) { DeviceDiagnostics.from(context) }
    val isDefaultHome = remember(context) { isDefaultHome(context) }
    val notificationAccess = remember(context) { notificationAccessEnabled(context) }

    SettingsScreen(
        state = stateHolder.uiState,
        diagnostics = diagnostics,
        isDefaultHome = isDefaultHome,
        notificationAccess = notificationAccess,
        onOpenHomeSettings = { openSystemSettings(context, Settings.ACTION_HOME_SETTINGS) },
        onStartOnBootChange = stateHolder::setStartOnBoot,
        onStartupScreenChange = stateHolder::setStartupScreen,
        onRailPositionChange = stateHolder::setRailPosition,
        onTextSizeChange = stateHolder::setTextSizePreset,
        onPreferredMediaAppChange = stateHolder::setPreferredMediaApp,
        onNavigationAppChange = stateHolder::setNavigationApp,
        onCompatibilityModeChange = stateHolder::setNavigationCompatibilityMode,
        onCopyDiagnostics = { copyDiagnostics(context, diagnostics.copyText) },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    diagnostics: DeviceDiagnostics,
    isDefaultHome: Boolean,
    notificationAccess: Boolean,
    onOpenHomeSettings: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onStartupScreenChange: (StartupScreen) -> Unit,
    onRailPositionChange: (RailPosition) -> Unit,
    onTextSizeChange: (TextSizePreset) -> Unit,
    onPreferredMediaAppChange: (LauncherApp?) -> Unit,
    onNavigationAppChange: (LauncherApp?) -> Unit,
    onCompatibilityModeChange: (Boolean) -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    var panel by rememberSaveable { mutableStateOf(SettingsPanel.Main.name) }
    val activePanel = runCatching { SettingsPanel.valueOf(panel) }.getOrDefault(SettingsPanel.Main)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (activePanel) {
            SettingsPanel.Main -> SettingsList(
                state = state,
                diagnostics = diagnostics,
                isDefaultHome = isDefaultHome,
                notificationAccess = notificationAccess,
                onOpenHomeSettings = onOpenHomeSettings,
                onStartOnBootChange = onStartOnBootChange,
                onOpenStartupScreen = { panel = SettingsPanel.StartupScreen.name },
                onOpenRailPosition = { panel = SettingsPanel.RailPosition.name },
                onOpenTextSize = { panel = SettingsPanel.TextSize.name },
                onOpenMediaApp = { panel = SettingsPanel.MediaApp.name },
                onOpenNavigationApp = { panel = SettingsPanel.NavigationApp.name },
                onCompatibilityModeChange = onCompatibilityModeChange,
                onCopyDiagnostics = onCopyDiagnostics,
                onOpenMaintenance = { panel = SettingsPanel.Maintenance.name },
            )

            SettingsPanel.Maintenance -> MaintenanceScreen(onBack = { panel = SettingsPanel.Main.name })

            SettingsPanel.StartupScreen -> OptionPicker(
                title = stringResource(R.string.settings_startup_screen),
                options = StartupScreen.entries.map { stringResource(it.labelRes) to (it == state.settings.startupScreen) },
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { index ->
                    onStartupScreenChange(StartupScreen.entries[index])
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.RailPosition -> OptionPicker(
                title = stringResource(R.string.settings_navigation_rail),
                options = RailPosition.entries.map { stringResource(it.labelRes) to (it == state.settings.railPosition) },
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { index ->
                    onRailPositionChange(RailPosition.entries[index])
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.TextSize -> OptionPicker(
                title = stringResource(R.string.settings_text_size),
                options = TextSizePreset.entries.map { stringResource(it.labelRes) to (it == state.settings.textSizePreset) },
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { index ->
                    onTextSizeChange(TextSizePreset.entries[index])
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.MediaApp -> AppPicker(
                title = stringResource(R.string.settings_preferred_media_app),
                apps = state.apps,
                selectedAppKey = state.settings.preferredMediaAppKey,
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { app ->
                    onPreferredMediaAppChange(app)
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.NavigationApp -> AppPicker(
                title = stringResource(R.string.settings_external_navigation_app),
                apps = state.apps,
                selectedAppKey = state.settings.navigationAppKey,
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { app ->
                    onNavigationAppChange(app)
                    panel = SettingsPanel.Main.name
                },
            )
        }
    }
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState,
    diagnostics: DeviceDiagnostics,
    isDefaultHome: Boolean,
    notificationAccess: Boolean,
    onOpenHomeSettings: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onOpenStartupScreen: () -> Unit,
    onOpenRailPosition: () -> Unit,
    onOpenTextSize: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onOpenNavigationApp: () -> Unit,
    onCompatibilityModeChange: (Boolean) -> Unit,
    onCopyDiagnostics: () -> Unit,
    onOpenMaintenance: () -> Unit,
) {
    val context = LocalContext.current
    var greetingEnabled by remember { mutableStateOf(WelcomePreferences.enabled(context)) }
    var greetingName by remember { mutableStateOf(WelcomePreferences.name(context)) }
    var editGreeting by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(greetingName) }
    if (editGreeting) {
        AlertDialog(
            onDismissRequest = { editGreeting = false },
            title = { Text("اسم الترحيب") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(30) },
                    label = { Text("الاسم") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    greetingName = nameDraft.trim()
                    WelcomePreferences.setName(context, greetingName)
                    editGreeting = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { editGreeting = false }) { Text("إلغاء") }
            },
        )
    }
    val settings = state.settings
    val resources = LocalContext.current.resources
    val chooseApp = stringResource(R.string.settings_choose_app)
    val unavailable = stringResource(R.string.settings_selected_app_unavailable)
    val mediaLabel = appLabel(state.apps, settings.preferredMediaAppKey, unavailable) ?: chooseApp
    val navigationLabel = appLabel(state.apps, settings.navigationAppKey, unavailable) ?: chooseApp

    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        CarHeader(title = stringResource(R.string.settings_title))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = CarSpacing.Xl),
        ) {
            section(resources.getString(R.string.settings_section_launcher))
            settingRow(
                title = resources.getString(R.string.settings_home_app),
                subtitle = if (isDefaultHome) {
                    resources.getString(R.string.settings_home_app_is_default)
                } else {
                    resources.getString(R.string.settings_home_app_choose)
                },
                onClick = onOpenHomeSettings,
                showChevron = true,
            )
            settingRow(
                title = resources.getString(R.string.settings_start_on_boot),
                subtitle = resources.getString(R.string.settings_start_on_boot_summary),
                onClick = { onStartOnBootChange(!settings.startOnBoot) },
                trailing = {
                    CarSwitch(
                        checked = settings.startOnBoot,
                        onCheckedChange = onStartOnBootChange,
                    )
                },
            )
            settingRow(
                title = resources.getString(R.string.settings_startup_screen),
                subtitle = resources.getString(settings.startupScreen.labelRes),
                onClick = onOpenStartupScreen,
                showChevron = true,
            )
            settingRow(
                title = resources.getString(R.string.settings_navigation_rail),
                subtitle = resources.getString(settings.railPosition.labelRes),
                onClick = onOpenRailPosition,
                showChevron = true,
            )
            settingRow(
                title = resources.getString(R.string.settings_pinned_apps),
                subtitle = resources.getString(
                    R.string.settings_pinned_apps_summary,
                    state.pinnedAppCount,
                    MaxPinnedApps,
                ),
            )

            settingRow(
                title = "الترحيب عند التشغيل",
                subtitle = "عرض رسالة ترحيبية عند فتح اللانشر",
                onClick = {
                    greetingEnabled = !greetingEnabled
                    WelcomePreferences.setEnabled(context, greetingEnabled)
                },
                trailing = {
                    CarSwitch(
                        checked = greetingEnabled,
                        onCheckedChange = {
                            greetingEnabled = it
                            WelcomePreferences.setEnabled(context, it)
                        },
                    )
                },
            )
            settingRow(
                title = "تغيير اسم الترحيب",
                subtitle = "أهلاً $greetingName",
                onClick = {
                    nameDraft = greetingName
                    editGreeting = true
                },
                showChevron = true,
            )

            section(resources.getString(R.string.settings_section_appearance))
            settingRow(
                title = resources.getString(R.string.settings_theme),
                subtitle = resources.getString(R.string.settings_theme_dark),
            )
            settingRow(
                title = resources.getString(R.string.settings_text_size),
                subtitle = resources.getString(settings.textSizePreset.labelRes),
                onClick = onOpenTextSize,
                showChevron = true,
            )

            section(resources.getString(R.string.settings_section_media))
            settingRow(
                title = resources.getString(R.string.settings_notification_access),
                subtitle = if (notificationAccess) {
                    resources.getString(R.string.settings_notification_access_granted)
                } else {
                    resources.getString(R.string.settings_notification_access_missing)
                },
            )
            settingRow(
                title = resources.getString(R.string.settings_preferred_media_app),
                subtitle = mediaLabel,
                onClick = onOpenMediaApp,
                showChevron = true,
            )

            section(resources.getString(R.string.settings_section_navigation))
            settingRow(
                title = resources.getString(R.string.settings_external_navigation_app),
                subtitle = navigationLabel,
                onClick = onOpenNavigationApp,
                showChevron = true,
            )
            settingRow(
                title = resources.getString(R.string.settings_embedded_navigation),
                subtitle = if (settings.navigationAppKey == null) {
                    resources.getString(R.string.settings_embedded_navigation_choose_first)
                } else {
                    resources.getString(R.string.settings_embedded_navigation_pending)
                },
            )
            settingRow(
                title = resources.getString(R.string.settings_compatibility_mode),
                subtitle = resources.getString(R.string.settings_compatibility_mode_summary),
                onClick = { onCompatibilityModeChange(!settings.navigationCompatibilityMode) },
                trailing = {
                    CarSwitch(
                        checked = settings.navigationCompatibilityMode,
                        onCheckedChange = onCompatibilityModeChange,
                    )
                },
            )

            section(resources.getString(R.string.settings_section_system))
            settingRow(
                title = "صيانة النظام",
                subtitle = "التخزين والذاكرة • ملفات APK • اكتشاف المكررات وحذفها بموافقتك",
                onClick = onOpenMaintenance,
                showChevron = true,
            )
            settingRow(
                title = "Car Launcher",
                subtitle = resources.getString(R.string.settings_diagnostics_version, diagnostics.appVersion),
            )
            settingRow(title = resources.getString(R.string.settings_diagnostics_android), subtitle = diagnostics.android)
            settingRow(title = resources.getString(R.string.settings_diagnostics_architecture), subtitle = diagnostics.abi)
            settingRow(title = resources.getString(R.string.settings_diagnostics_display), subtitle = diagnostics.display)
            settingRow(title = resources.getString(R.string.settings_diagnostics_permissions), subtitle = diagnostics.permissions)
            settingRow(
                title = resources.getString(R.string.settings_diagnostics_copy),
                subtitle = resources.getString(R.string.settings_diagnostics_copy_summary),
                onClick = onCopyDiagnostics,
                showChevron = true,
            )
        }
    }
}

@Composable
private fun OptionPicker(
    title: String,
    options: List<Pair<String, Boolean>>,
    onBack: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        SettingsSubpageHeader(title = title, onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(options.size) { index ->
                val (label, selected) = options[index]
                settingRowContent(
                    title = label,
                    onClick = { onSelected(index) },
                    trailing = if (selected) selectedIcon() else null,
                )
            }
        }
    }
}

@Composable
private fun AppPicker(
    title: String,
    apps: List<LauncherApp>,
    selectedAppKey: String?,
    onBack: () -> Unit,
    onSelected: (LauncherApp?) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        SettingsSubpageHeader(title = title, onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = CarSpacing.Lg),
        ) {
            item(key = "none") {
                settingRowContent(
                    title = stringResource(R.string.settings_none),
                    subtitle = stringResource(R.string.settings_none_summary),
                    onClick = { onSelected(null) },
                    trailing = if (selectedAppKey == null) selectedIcon() else null,
                )
            }
            items(items = apps, key = LauncherApp::stableKey) { app ->
                settingRowContent(
                    title = app.label,
                    subtitle = app.packageName,
                    onClick = { onSelected(app) },
                    trailing = if (app.stableKey == selectedAppKey) selectedIcon() else null,
                )
            }
        }
    }
}

@Composable
private fun SettingsSubpageHeader(title: String, onBack: () -> Unit) {
    CarHeader(
        title = title,
        leading = {
            CarIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                onClick = onBack,
            )
        },
    )
}

private fun LazyListScope.section(title: String) {
    item(key = "section-$title") {
        Text(
            text = title,
            modifier = Modifier.padding(
                start = CarSpacing.Lg,
                top = CarSpacing.Xl,
                bottom = CarSpacing.Sm,
            ),
            color = CarColors.AccentMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun LazyListScope.settingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    item(key = "row-$title") {
        settingRowContent(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailing = trailing ?: if (showChevron) chevron() else null,
        )
    }
}

@Composable
private fun settingRowContent(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CarListRow(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailing = trailing,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = CarSpacing.Lg),
            thickness = 1.dp,
            color = CarColors.Outline,
        )
    }
}

private fun chevron(): @Composable () -> Unit = {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = CarColors.TextSecondary,
    )
}

private fun selectedIcon(): @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = stringResource(R.string.settings_selected),
        tint = CarColors.AccentMuted,
    )
}

private fun appLabel(apps: List<LauncherApp>, appKey: String?, unavailable: String): String? {
    if (appKey == null) return null
    return apps.firstOrNull { it.stableKey == appKey }?.label ?: unavailable
}

data class DeviceDiagnostics(
    val appVersion: String,
    val android: String,
    val abi: String,
    val display: String,
    val permissions: String,
    private val copyAbiLine: String = "ABI: $abi",
    private val copyDisplayLine: String = "Display: $display",
    private val copyPermissionsLine: String = "Permissions: $permissions",
) {
    val copyText: String = listOf(
        "Car Launcher $appVersion",
        android,
        copyAbiLine,
        copyDisplayLine,
        copyPermissionsLine,
    ).joinToString("\n")

    companion object {
        @Suppress("DEPRECATION")
        fun from(context: Context): DeviceDiagnostics {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )
            val metrics = realDisplayMetrics(context)
            val requestedPermissions = packageInfo.requestedPermissions.orEmpty()
            val grantedPermissions = requestedPermissions.count { permission ->
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }
            val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
            val displayText = "${metrics.widthPixels} × ${metrics.heightPixels} · ${metrics.densityDpi} dpi"
            val permissionsText = context.getString(
                R.string.diagnostics_permissions_value,
                grantedPermissions,
                requestedPermissions.size,
            )
            return DeviceDiagnostics(
                appVersion = packageInfo.versionName.orEmpty(),
                android = context.getString(
                    R.string.diagnostics_android_value,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                ),
                abi = abi,
                display = displayText,
                permissions = permissionsText,
                copyAbiLine = context.getString(R.string.diagnostics_copy_abi, abi),
                copyDisplayLine = context.getString(R.string.diagnostics_copy_display, displayText),
                copyPermissionsLine = context.getString(R.string.diagnostics_copy_permissions, permissionsText),
            )
        }
    }
}

private enum class SettingsPanel {
    Main,
    Maintenance,
    StartupScreen,
    RailPosition,
    TextSize,
    MediaApp,
    NavigationApp,
}

private val SettingsContentWidth = 1032.dp

private fun isDefaultHome(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun notificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    return enabledListeners
        .split(':')
        .any { component -> component.substringBefore('/').equals(context.packageName, ignoreCase = true) }
}

private fun openSystemSettings(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}

private fun copyDiagnostics(context: Context, diagnostics: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.diagnostics_clip_label), diagnostics))
    Toast.makeText(context, context.getString(R.string.settings_diagnostics_copied), Toast.LENGTH_SHORT).show()
}

@Suppress("DEPRECATION")
private fun realDisplayMetrics(context: Context): DisplayMetrics {
    return DisplayMetrics().also { metrics ->
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
    }
}
