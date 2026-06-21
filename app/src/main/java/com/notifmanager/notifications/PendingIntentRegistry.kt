package com.notifmanager.notifications

import android.app.PendingIntent

object PendingIntentRegistry {
    private val intents = mutableMapOf<String, PendingIntent>()

    @Synchronized
    fun put(notificationKey: String, pendingIntent: PendingIntent?) {
        if (pendingIntent != null) {
            intents[notificationKey] = pendingIntent
        }
    }

    @Synchronized
    fun remove(notificationKey: String) {
        intents.remove(notificationKey)
    }

    @Synchronized
    fun send(notificationKey: String): Boolean {
        val pendingIntent = intents[notificationKey] ?: return false
        return runCatching {
            pendingIntent.send()
            true
        }.getOrDefault(false)
    }
}
