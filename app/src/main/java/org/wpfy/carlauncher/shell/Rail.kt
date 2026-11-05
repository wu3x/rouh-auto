package org.wpfy.carlauncher.shell

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.data.persistence.MaxPinnedApps
import org.wpfy.carlauncher.design.component.CarRailButton
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarDimensions
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.launcher.LauncherApp
import org.wpfy.carlauncher.launcher.rememberAppsStateHolder

enum class RailOrientation {
    Horizontal,
    Vertical,
}

@Composable
fun Rail(
    selected: ShellDestination,
    onDestinationSelected: (ShellDestination) -> Unit,
    notificationCount: Int = 0,
    onAssistantClick: () -> Unit = {},
    orientation: RailOrientation = RailOrientation.Vertical,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val appsStateHolder = rememberAppsStateHolder()
    val pinnedApps = appsStateHolder.uiState.apps
        .filter(appsStateHolder.uiState::isPinned)
        .take(MaxDockApps)
    val dockApps = if (pinnedApps.isNotEmpty() || !isDebuggable) {
        pinnedApps
    } else {
        appsStateHolder.uiState.apps.take(MaxDockApps)
    }

    if (orientation == RailOrientation.Horizontal) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = CarSpacing.Md, vertical = CarSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            AppLauncherButton(selected, onDestinationSelected)
            CarRailButton(
                icon = Icons.Rounded.Mic,
                contentDescription = "Digital assistant",
                selected = false,
                onClick = onAssistantClick,
            )
            AppDock(
                apps = dockApps,
                onAppClick = appsStateHolder::launch,
                iconLoader = appsStateHolder::loadIcon,
                horizontal = true,
            )
            Spacer(modifier = Modifier.weight(1f))
            NotificationRailButton(
                count = notificationCount,
                selected = selected == ShellDestination.Notifications,
                onClick = {
                    onDestinationSelected(
                        if (selected == ShellDestination.Notifications) {
                            ShellDestination.Home
                        } else {
                            ShellDestination.Notifications
                        },
                    )
                },
            )
            StatusArea()
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight()
                .background(Color.Black)
                .padding(vertical = CarSpacing.Xl),
        ) {
            // Top group (status + bell) and bottom group (mic + apps) each use two action slots.
            val reservedHeight = CarDimensions.RailActionSize * 4 + RailItemSpacing * 2 + CarSpacing.Lg * 2
            val dockItemSize = if (dockApps.isEmpty()) {
                CarDimensions.RailActionSize
            } else {
                ((maxHeight - reservedHeight) / dockApps.size)
                    .coerceIn(MinDockItemSize, CarDimensions.RailActionSize)
            }
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RailItemSpacing),
            ) {
                StatusArea()
                NotificationRailButton(
                    count = notificationCount,
                    selected = selected == ShellDestination.Notifications,
                    onClick = {
                        onDestinationSelected(
                            if (selected == ShellDestination.Notifications) {
                                ShellDestination.Home
                            } else {
                                ShellDestination.Notifications
                            },
                        )
                    },
                )
            }

            AppDock(
                apps = dockApps,
                onAppClick = appsStateHolder::launch,
                iconLoader = appsStateHolder::loadIcon,
                horizontal = false,
                itemSize = dockItemSize,
                modifier = Modifier.align(Alignment.Center),
            )

            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RailItemSpacing),
            ) {
                CarRailButton(
                    icon = Icons.Rounded.Mic,
                    contentDescription = "Digital assistant",
                    selected = false,
                    onClick = onAssistantClick,
                )
                AppLauncherButton(selected, onDestinationSelected)
            }
        }
    }
}

@Composable
private fun AppDock(
    apps: List<LauncherApp>,
    onAppClick: (LauncherApp) -> Unit,
    iconLoader: suspend (LauncherApp) -> ImageBitmap?,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
    itemSize: Dp = CarDimensions.RailActionSize,
) {
    if (apps.isEmpty()) return

    if (horizontal) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                DockAppButton(app, onAppClick, iconLoader, itemSize)
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            apps.forEach { app ->
                DockAppButton(app, onAppClick, iconLoader, itemSize)
            }
        }
    }
}

@Composable
private fun DockAppButton(
    app: LauncherApp,
    onAppClick: (LauncherApp) -> Unit,
    iconLoader: suspend (LauncherApp) -> ImageBitmap?,
    itemSize: Dp = CarDimensions.RailActionSize,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = app.stableKey) {
        value = iconLoader(app)
    }

    Box(
        modifier = Modifier
            .size(itemSize)
            .clickable(onClick = { onAppClick(app) }),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon!!,
                contentDescription = app.label,
                modifier = Modifier.size(itemSize * 0.625f),
            )
        }
    }
}

@Composable
private fun AppLauncherButton(
    selected: ShellDestination,
    onDestinationSelected: (ShellDestination) -> Unit,
) {
    val destination = appLauncherDestination(selected)
    val onHome = destination == ShellDestination.Home
    CarRailButton(
        icon = if (onHome) Icons.Rounded.Home else Icons.Rounded.Apps,
        contentDescription = if (onHome) "Go home" else "Open app drawer",
        selected = selected == ShellDestination.Home,
        onClick = { onDestinationSelected(destination) },
    )
}

internal fun appLauncherDestination(selected: ShellDestination): ShellDestination {
    return if (selected == ShellDestination.Home) {
        ShellDestination.Apps
    } else {
        ShellDestination.Home
    }
}

@Composable
private fun NotificationRailButton(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(CarDimensions.RailActionSize)) {
        CarRailButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = if (selected) "Go home" else "Open notifications",
            selected = selected,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(CarColors.AccentMuted, CarShapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = Color(0xFF002C6F),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val MaxDockApps = MaxPinnedApps
private val RailItemSpacing = 2.dp
private val MinDockItemSize = 44.dp
