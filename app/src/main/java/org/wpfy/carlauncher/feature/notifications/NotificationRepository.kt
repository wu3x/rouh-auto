package org.wpfy.carlauncher.feature.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface NotificationRepository {
    val state: StateFlow<NotificationState>

    fun start()
    fun stop()
    fun refresh()
    fun dismiss(key: String)
    fun clearAllVisible()
}

class AndroidNotificationRepository(context: Context) : NotificationRepository {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listenerComponent = ComponentName(
        appContext,
        LauncherNotificationListenerService::class.java,
    )
    private val appInfoCache = mutableMapOf<String, Pair<String, Bitmap?>>()

    private val _state = MutableStateFlow(NotificationState())
    override val state: StateFlow<NotificationState> = _state.asStateFlow()

    private var started = false
    private var accessTicker: Job? = null

    private val listener: () -> Unit = { refresh() }

    override fun start() {
        if (started) return
        started = true
        NotificationListenerBridge.addListener(listener)
        refresh()
        accessTicker = scope.launch {
            while (isActive) {
                refresh()
                delay(1_500L)
            }
        }
    }

    override fun stop() {
        if (!started) return
        started = false
        accessTicker?.cancel()
        accessTicker = null
        NotificationListenerBridge.removeListener(listener)
    }

    override fun refresh() {
        scope.launch {
            if (!hasAccess()) {
                _state.value = NotificationState(
                    accessStatus = NotificationAccessStatus.PermissionRequired,
                )
                return@launch
            }

            val service = NotificationListenerBridge.service
            if (service == null) {
                runCatching { NotificationListenerService.requestRebind(listenerComponent) }
                _state.value = NotificationState(
                    accessStatus = NotificationAccessStatus.Available,
                )
                return@launch
            }

            val active = runCatching { service.activeNotifications?.toList().orEmpty() }
                .getOrDefault(emptyList())
            _state.value = NotificationState(
                accessStatus = NotificationAccessStatus.Available,
                notifications = active
                    .asSequence()
                    .filter(::isSafeForDrivingDisplay)
                    .mapNotNull(::toLauncherNotification)
                    .sortedByDescending(LauncherNotification::postTimeMillis)
                    .toList(),
            )
        }
    }

    override fun dismiss(key: String) {
        val notification = _state.value.notifications.firstOrNull { it.key == key } ?: return
        if (!notification.isClearable) return
        runCatching { NotificationListenerBridge.service?.cancelNotification(key) }
        refresh()
    }

    override fun clearAllVisible() {
        val service = NotificationListenerBridge.service ?: return
        _state.value.notifications
            .asSequence()
            .filter(LauncherNotification::isClearable)
            .forEach { notification ->
                runCatching { service.cancelNotification(notification.key) }
            }
        refresh()
    }

    private fun hasAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName)
    }

    private fun isSafeForDrivingDisplay(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (notification.visibility == Notification.VISIBILITY_SECRET) return false
        if (notification.category == Notification.CATEGORY_TRANSPORT) return false
        if (notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) return false
        if (notification.category !in SafeCategories) return false

        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()
        return title.isNotBlank() || text.isNotBlank()
    }

    private fun toLauncherNotification(sbn: StatusBarNotification): LauncherNotification? {
        val notification = sbn.notification ?: return null
        val (appLabel, appIcon) = appInfo(sbn.packageName)
        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()
            .orEmpty()
            .ifBlank { appLabel }
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        return LauncherNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel,
            appIcon = appIcon,
            title = title.take(MaxTitleLength),
            text = text.take(MaxTextLength),
            postTimeMillis = sbn.postTime,
            isClearable = sbn.isClearable,
        )
    }

    private fun appInfo(packageName: String): Pair<String, Bitmap?> {
        appInfoCache[packageName]?.let { return it }

        return runCatching {
            val packageManager = appContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(applicationInfo)
                .toString()
                .ifBlank { packageName }
            val icon = runCatching {
                packageManager.getApplicationIcon(applicationInfo).toBitmap(56, 56)
            }.getOrNull()
            label to icon
        }.getOrElse {
            packageName to null
        }.also { appInfoCache[packageName] = it }
    }

    companion object {
        private const val MaxTitleLength = 120
        private const val MaxTextLength = 240

        private val SafeCategories = setOf(
            Notification.CATEGORY_MESSAGE,
            Notification.CATEGORY_EMAIL,
            Notification.CATEGORY_EVENT,
            Notification.CATEGORY_REMINDER,
            "navigation",
        )
    }
}
