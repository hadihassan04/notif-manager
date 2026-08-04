package com.tide.app.notifications

import android.app.Notification
import android.os.Build
import android.os.Process
import android.os.UserHandle
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
        backfillChannelNames()
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
            channelName = channelNameFor(sbn),
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

    /**
     * The user-visible channel name, which is what Android's own notification settings
     * show. `Notification.channelId` is a developer string ("chat_messages_v2"), so it
     * is only a fallback. The ranking already carries the channel for a notification
     * that was just posted; querying the app's channels is the older path.
     */
    private fun channelNameFor(sbn: StatusBarNotification): String? {
        val channelId = sbn.notification.channelId ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val ranking = Ranking()
            if (currentRanking?.getRanking(sbn.key, ranking) == true) {
                ranking.channel?.name?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return channelNames(sbn.packageName, sbn.user)[channelId]
    }

    private fun channelNames(packageName: String, user: UserHandle): Map<String, String> {
        return runCatching {
            getNotificationChannels(packageName, user)
                .mapNotNull { channel ->
                    val name = channel.name?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    channel.id to name
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    /**
     * Notifications captured before names were resolved still show their channel id.
     * The listener is the only place that can read another app's channels, so name them
     * here, once, whenever it connects.
     */
    private fun backfillChannelNames() {
        scope.launch {
            val repository = (application as TideApp).repository
            val user = Process.myUserHandle()
            repository.packagesMissingChannelNames().forEach { packageName ->
                repository.nameChannels(packageName, channelNames(packageName, user))
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
