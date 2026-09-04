package com.tide.app.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.tide.app.data.NotificationEntity

/**
 * Shade helpers for Tide. Batch **release** is Inbox-only — it must not post
 * cards to the Android notification bar. [release] only clears leftovers from
 * older builds that re-posted on release.
 */
class NotificationPublisher(private val context: Context) {
    fun release(notifications: List<NotificationEntity>) {
        notifications.forEach { cancel(context, it.notificationKey) }
    }

    companion object {
        const val RELEASED_TAG = "released"

        fun shadeId(notificationKey: String): Int = notificationKey.hashCode()

        fun cancel(context: Context, notificationKey: String) {
            NotificationManagerCompat.from(context).cancel(RELEASED_TAG, shadeId(notificationKey))
        }
    }
}
