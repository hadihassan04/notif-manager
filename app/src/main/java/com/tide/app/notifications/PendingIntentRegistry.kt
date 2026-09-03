package com.tide.app.notifications

import android.app.PendingIntent
import android.os.Bundle

object PendingIntentRegistry {
    private val intents = mutableMapOf<String, PendingIntent>()

    @Synchronized
    fun put(notificationKey: String, pendingIntent: PendingIntent?) {
        if (pendingIntent != null) {
            intents[notificationKey] = pendingIntent
        }
    }

    @Synchronized
    fun get(notificationKey: String): PendingIntent? = intents[notificationKey]

    @Synchronized
    fun remove(notificationKey: String) {
        intents.remove(notificationKey)
    }

    @Synchronized
    fun contains(notificationKey: String): Boolean = intents.containsKey(notificationKey)

    @Synchronized
    fun send(notificationKey: String, options: Bundle? = null): Boolean {
        val pendingIntent = intents[notificationKey] ?: return false
        return runCatching {
            if (options != null) {
                pendingIntent.send(null, 0, null, null, null, null, options)
            } else {
                pendingIntent.send()
            }
            true
        }.getOrDefault(false)
    }
}
