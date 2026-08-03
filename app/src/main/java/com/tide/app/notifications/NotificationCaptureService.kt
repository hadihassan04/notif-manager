package com.tide.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tide.app.TideApp
import com.tide.app.core.IncomingNotification
import com.tide.app.data.DeliveryMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val canceler: (String) -> Unit = { key -> cancelNotification(key) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationStatusController.register(canceler)
    }

    override fun onListenerDisconnected() {
        NotificationStatusController.unregister(canceler)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val appProfile = NotificationCaptureFilter.appProfile(packageManager, sbn.packageName)
        if (!NotificationCaptureFilter.shouldStore(sbn, appProfile, title, text)) return

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
            batchesByDefault = appProfile.batchesByDefault,
            isMediaPlayback = NotificationCaptureFilter.isMediaPlayback(sbn, appProfile),
        )

        scope.launch {
            val entity = (application as TideApp).repository.capture(incoming)
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
}
