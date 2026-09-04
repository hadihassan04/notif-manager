package com.tide.app.notifications

import com.tide.app.data.NotificationEntity

enum class ReleasedTap {
    /** Shade [android.app.Notification.contentIntent] is the captured app PendingIntent. */
    CAPTURED_INTENT,
    /** Shade contentIntent is Tide's Activity trampoline; original token is gone. */
    TRAMPOLINE,
}

data class ReleasedNotificationSpec(
    val notificationKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String?,
    val tap: ReleasedTap,
)

/**
 * Specs for opening a captured notification from Inbox. Release itself does
 * not post shade cards — items appear in Inbox only.
 */
object ReleasedNotificationPlan {
    fun specs(
        notifications: List<NotificationEntity>,
        hasCapturedIntent: (String) -> Boolean,
    ): List<ReleasedNotificationSpec> {
        return notifications.map { item ->
            val hasIntent = hasCapturedIntent(item.notificationKey)
            ReleasedNotificationSpec(
                notificationKey = item.notificationKey,
                packageName = item.packageName,
                appLabel = item.appLabel,
                title = item.title?.takeIf { it.isNotBlank() } ?: item.appLabel,
                text = item.text,
                tap = if (hasIntent) ReleasedTap.CAPTURED_INTENT else ReleasedTap.TRAMPOLINE,
            )
        }
    }

    fun fallbackMessage(openedApp: Boolean, appLabel: String): String {
        return if (openedApp) {
            "Opening $appLabel."
        } else {
            "Couldn't open that notification."
        }
    }
}
