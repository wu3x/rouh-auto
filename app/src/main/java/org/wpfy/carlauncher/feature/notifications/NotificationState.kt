package org.wpfy.carlauncher.feature.notifications

import android.graphics.Bitmap

enum class NotificationAccessStatus {
    PermissionRequired,
    Available,
}

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val appIcon: Bitmap? = null,
    val title: String,
    val text: String,
    val postTimeMillis: Long,
    val isClearable: Boolean,
)

data class NotificationState(
    val accessStatus: NotificationAccessStatus = NotificationAccessStatus.PermissionRequired,
    val notifications: List<LauncherNotification> = emptyList(),
)
