package org.wpfy.carlauncher.feature.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArraySet

class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationListenerBridge.attach(this)
    }

    override fun onListenerDisconnected() {
        NotificationListenerBridge.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NotificationListenerBridge.notifyChanged()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NotificationListenerBridge.notifyChanged()
    }
}

internal object NotificationListenerBridge {
    @Volatile
    var service: LauncherNotificationListenerService? = null
        private set

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun attach(service: LauncherNotificationListenerService) {
        this.service = service
        notifyChanged()
    }

    fun detach(service: LauncherNotificationListenerService) {
        if (this.service === service) {
            this.service = null
        }
        notifyChanged()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun notifyChanged() {
        listeners.forEach { it.invoke() }
    }
}
