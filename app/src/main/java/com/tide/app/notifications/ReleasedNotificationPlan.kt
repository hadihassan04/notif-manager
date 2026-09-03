package com.tide.app.notifications

import com.tide.app.data.NotificationEntity

enum class ReleasedTap {
    CAPTURED_INTENT,
    OPEN_APP,
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
 * A batch release is one shade card per captured notification, each tapping
 * through the stored content PendingIntent when it still exists. There is no
 * Tide summary titled "Waiting notifications delivered".
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
                text = if (hasIntent) item.text else fallbackText(item.text, item.appLabel),
                tap = if (hasIntent) ReleasedTap.CAPTURED_INTENT else ReleasedTap.OPEN_APP,
            )
        }
    }

    fun fallbackText(originalText: String?, appLabel: String): String {
        val body = originalText?.takeIf { it.isNotBlank() }
        val note = "Original tap expired. Opens $appLabel."
        return if (body == null) note else "$body\n$note"
    }

    fun fallbackMessage(openedApp: Boolean, appLabel: String): String {
        return if (openedApp) {
            "Original tap expired. Opening $appLabel."
        } else {
            "Original notification action expired."
        }
    }
}
