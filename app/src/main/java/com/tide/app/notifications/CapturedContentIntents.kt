package com.tide.app.notifications

import android.app.Notification
import android.app.PendingIntent

/**
 * Tokens that can actually open the source notification. Never invent a deep
 * link; only reuse intents Android already attached to the StatusBarNotification.
 */
object CapturedContentIntents {
    fun from(notification: Notification): PendingIntent? {
        return prefer(notification.contentIntent, notification.publicVersion?.contentIntent)
    }

    fun <T> prefer(contentIntent: T?, publicVersionIntent: T?): T? {
        return contentIntent ?: publicVersionIntent
    }
}
