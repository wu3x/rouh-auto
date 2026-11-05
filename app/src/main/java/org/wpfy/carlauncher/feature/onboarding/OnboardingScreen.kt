package org.wpfy.carlauncher.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.design.component.CarListRow
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.feature.settings.SettingsStateHolder

@Composable
fun OnboardingRoute(stateHolder: SettingsStateHolder) {
    var stepName by rememberSaveable { mutableStateOf(OnboardingStep.Setup.name) }
    val step = runCatching { OnboardingStep.valueOf(stepName) }.getOrDefault(OnboardingStep.Setup)
    var selectedRailName by rememberSaveable { mutableStateOf<String?>(null) }

    // Skip is intended for emulator/desktop testing. Permissions can be granted later in Settings.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    when (step) {
        OnboardingStep.Setup -> SetupScreen(
            onContinue = { stepName = nextOnboardingStep(OnboardingStep.Setup).name },
            onSkip = { stateHolder.completeOnboarding(RailPosition.Left) },
        )

        OnboardingStep.RailPosition -> RailPositionScreen(
            selectedPosition = selectedRailName?.let { name ->
                runCatching { RailPosition.valueOf(name) }.getOrNull()
            },
            onSelected = { selectedRailName = it.name },
            onBack = { stepName = OnboardingStep.Setup.name },
            onContinue = { position -> stateHolder.completeOnboarding(position) },
        )
    }
    }
}

@Composable
private fun SetupScreen(onContinue: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    var refreshGeneration by remember { mutableIntStateOf(0) }
    var locationRequestAttempted by rememberSaveable { mutableStateOf(false) }
    val status = remember(context, refreshGeneration) { readSetupStatus(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshGeneration += 1
    }

    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshGeneration += 1
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    OnboardingScaffold(
        title = stringResource(R.string.ob_setup_title, context.getString(R.string.app_name)),
        subtitle = stringResource(R.string.ob_setup_subtitle),
        continueEnabled = status.allSatisfied,
        onContinue = onContinue,
        onSkip = onSkip,
    ) {
        CapabilityRow(
            title = stringResource(R.string.ob_location_title),
            subtitle = if (status.preciseLocation) {
                stringResource(R.string.ob_granted)
            } else {
                stringResource(R.string.ob_location_required)
            },
            satisfied = status.preciseLocation,
            onClick = {
                if (status.preciseLocation) return@CapabilityRow
                val permanentlyDenied = locationRequestAttempted &&
                    activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == false
                if (permanentlyDenied) {
                    openAppSettings(context)
                } else {
                    locationRequestAttempted = true
                    locationPermissionLauncher.launch(onboardingLocationPermissions())
                }
            },
        )
        CapabilityRow(
            title = stringResource(R.string.ob_notification_title),
            subtitle = if (status.notificationAccess) {
                stringResource(R.string.ob_granted)
            } else {
                stringResource(R.string.ob_notification_required)
            },
            satisfied = status.notificationAccess,
            onClick = { openSystemSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
        )
        CapabilityRow(
            title = stringResource(R.string.ob_home_title),
            subtitle = if (status.defaultHome) {
                stringResource(R.string.ob_home_yes)
            } else {
                stringResource(R.string.ob_home_no)
            },
            satisfied = status.defaultHome,
            onClick = { openSystemSettings(context, Settings.ACTION_HOME_SETTINGS) },
        )
    }
}

internal fun onboardingLocationPermissions(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@Composable
private fun RailPositionScreen(
    selectedPosition: RailPosition?,
    onSelected: (RailPosition) -> Unit,
    onBack: () -> Unit,
    onContinue: (RailPosition) -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.ob_rail_title),
        subtitle = stringResource(R.string.ob_rail_subtitle),
        onBack = onBack,
        continueEnabled = selectedPosition != null,
        onContinue = { selectedPosition?.let(onContinue) },
        continueLabel = stringResource(R.string.ob_finish),
    ) {
        RailPosition.entries.forEach { position ->
            CarListRow(
                title = stringResource(position.labelRes),
                subtitle = when (position) {
                    RailPosition.Left -> stringResource(R.string.ob_rail_left_desc)
                    RailPosition.Right -> stringResource(R.string.ob_rail_right_desc)
                    RailPosition.Bottom -> stringResource(R.string.ob_rail_bottom_desc)
                },
                onClick = { onSelected(position) },
                trailing = {
                    val selected = position == selectedPosition
                    SelectionIcon(
                        selected = selected,
                        contentDescription = stringResource(if (selected) R.string.ob_selected else R.string.ob_not_selected),
                    )
                },
            )
        }
    }
}

@Composable
private fun OnboardingScaffold(
    title: String,
    subtitle: String,
    continueEnabled: Boolean,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    continueLabel: String = stringResource(R.string.ob_continue),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .widthIn(max = 1032.dp),
    ) {
        Text(
            text = title,
            color = CarColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = subtitle,
            color = CarColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = CarSpacing.Sm, bottom = CarSpacing.Lg),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = CarSpacing.Lg),
        ) {
            item { content() }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
                Spacer(modifier = Modifier.weight(1f))
            }
            if (onSkip != null) {
                TextButton(onClick = onSkip) {
                    Text("تخطي")
                }
                Spacer(modifier = Modifier.widthIn(min = 12.dp))
            }
            Button(
                onClick = onContinue,
                enabled = continueEnabled,
            ) {
                Text(continueLabel)
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    subtitle: String,
    satisfied: Boolean,
    onClick: () -> Unit,
) {
    CarListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            SelectionIcon(
                selected = satisfied,
                contentDescription = stringResource(if (satisfied) R.string.ob_complete else R.string.ob_not_complete),
            )
        },
    )
}

@Composable
private fun SelectionIcon(
    selected: Boolean,
    contentDescription: String,
) {
    Icon(
        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = contentDescription,
        tint = if (selected) CarColors.AccentMuted else CarColors.TextSecondary,
    )
}

internal fun selectionStatusDescription(selected: Boolean): String =
    if (selected) "Selected" else "Not selected"

internal fun capabilityStatusDescription(satisfied: Boolean): String =
    if (satisfied) "Complete" else "Not complete"

private fun readSetupStatus(context: Context): SetupCapabilityStatus {
    return SetupCapabilityStatus(
        preciseLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED,
        notificationAccess = notificationAccessEnabled(context),
        defaultHome = isDefaultHome(context),
    )
}

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

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    runCatching { context.startActivity(intent) }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is ComponentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
