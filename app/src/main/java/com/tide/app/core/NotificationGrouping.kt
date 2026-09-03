package com.tide.app.core

import com.tide.app.data.NotificationEntity

/**
 * Inbox rows are one captured notification each. Distinct WhatsApp (or any
 * other) threads stay distinct even when they share an app, channel, or title.
 */
object NotificationGrouping {
    fun rowKey(item: NotificationEntity): String = item.notificationKey

    fun rows(items: List<NotificationEntity>): List<NotificationEntity> {
        return items.sortedByDescending { it.postedAtMillis }
    }
}
