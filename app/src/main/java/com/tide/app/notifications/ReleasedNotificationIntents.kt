package com.tide.app.notifications

/**
 * How a released shade card is tapped. SystemUI delivers [contentIntent] with
 * notification-click privileges; wrapping that through a Tide BroadcastReceiver
 * is a background start and no-ops on modern Android.
 */
object ReleasedNotificationIntents {
    const val EXTRA_NOTIFICATION_KEY = "notification_key"
    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_APP_LABEL = "app_label"

    /**
     * Prefer the captured WhatsApp/Reddit/etc PendingIntent. Only fall back to
     * Tide's Activity trampoline when that token is gone (process death).
     */
    fun <T : Any> shadeContentIntent(captured: T?, trampoline: T): T = captured ?: trampoline
}
