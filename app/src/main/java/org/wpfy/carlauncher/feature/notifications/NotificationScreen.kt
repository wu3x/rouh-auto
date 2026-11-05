package org.wpfy.carlauncher.feature.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.design.theme.CarSpacing
import java.util.concurrent.TimeUnit

@Composable
fun NotificationRoute(
    state: NotificationState,
    repository: NotificationRepository,
) {
    val context = LocalContext.current
    NotificationScreen(
        state = state,
        onRequestAccess = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        onDismiss = repository::dismiss,
        onClearAll = repository::clearAllVisible,
    )
}

@Composable
fun NotificationScreen(
    state: NotificationState,
    onRequestAccess: () -> Unit,
    onDismiss: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    when {
        state.accessStatus == NotificationAccessStatus.PermissionRequired -> NotificationAccessRequired(
            onRequestAccess = onRequestAccess,
        )

        state.notifications.isEmpty() -> NotificationEmpty()

        else -> NotificationList(
            notifications = state.notifications,
            onDismiss = onDismiss,
            onClearAll = onClearAll,
        )
    }
}

@Composable
private fun NotificationList(
    notifications: List<LauncherNotification>,
    onDismiss: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 80.dp,
            end = 80.dp,
            top = 24.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = notifications,
            key = LauncherNotification::key,
        ) { notification ->
            NotificationCard(
                notification = notification,
                onDismiss = { onDismiss(notification.key) },
            )
        }

        if (notifications.any(LauncherNotification::isClearable)) {
            item(key = "clear-all") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(28.dp),
                        color = CarColors.SurfaceElevated,
                    ) {
                        Text(
                            text = "Clear all",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            color = CarColors.TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: LauncherNotification,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CarColors.SurfaceContainer,
        shape = CarShapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                NotificationAppIcon(notification)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            append(notification.title)
                            append(" · ")
                            append(relativeTime(notification.postTimeMillis))
                        },
                        color = CarColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = notification.appLabel,
                        color = CarColors.TextDisabled,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (notification.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.text,
                            color = CarColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (notification.isClearable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier.widthIn(min = 112.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = CarColors.SurfaceElevated,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = CarColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Dismiss",
                                color = CarColors.TextPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAppIcon(notification: LauncherNotification) {
    val icon = notification.appIcon
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(CarColors.SurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = CarColors.AccentMuted,
            )
        }
    }
}

@Composable
private fun NotificationAccessRequired(onRequestAccess: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(CarSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CarSpacing.Md),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = CarColors.AccentMuted,
            )
            Text(
                text = "Allow notification access",
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Enable Car Launcher under Notification access to show a driving-safe notification list.",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Surface(
                onClick = onRequestAccess,
                shape = RoundedCornerShape(36.dp),
                color = CarColors.AccentMuted,
            ) {
                Text(
                    text = "Open notification access",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    color = Color(0xFF002C6F),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NotificationEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CarColors.TextDisabled,
            )
            Text(
                text = "No driving-safe notifications",
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Messages, reminders, events, and navigation alerts will appear here.",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun relativeTime(postTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val age = (nowMillis - postTimeMillis).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(age)
    return when {
        minutes < 1L -> "now"
        minutes < 60L -> "${minutes}m"
        minutes < 1_440L -> "${minutes / 60L}h"
        else -> "${minutes / 1_440L}d"
    }
}
