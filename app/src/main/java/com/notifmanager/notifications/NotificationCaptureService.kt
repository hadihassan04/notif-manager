package com.notifmanager.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notifmanager.NotifManagerApp
import com.notifmanager.core.IncomingNotification
import com.notifmanager.data.DeliveryMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (isIgnoredNotification(sbn.packageName, title, text)) return

        PendingIntentRegistry.put(sbn.key, sbn.notification.contentIntent)
        val incoming = IncomingNotification(
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabelFor(sbn.packageName),
            title = title,
            text = text,
            channelId = sbn.notification.channelId,
            channelName = sbn.notification.channelId,
            category = sbn.notification.category,
            postedAtMillis = sbn.postTime,
        )

        scope.launch {
            val entity = (application as NotifManagerApp).repository.capture(incoming)
            if (entity.deliveryMode == DeliveryMode.BATCH) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun appLabelFor(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            info.loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
    }

    private fun isIgnoredNotification(packageName: String, title: String?, text: String?): Boolean {
        if (!packageName.contains("whatsapp", ignoreCase = true)) return false
        val content = listOfNotNull(title, text).joinToString(" ").lowercase()
        return content.contains("checking for new messages") ||
            content.contains("checking for messages")
    }
}
