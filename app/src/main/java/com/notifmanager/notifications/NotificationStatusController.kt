package com.notifmanager.notifications

object NotificationStatusController {
    private var canceler: ((String) -> Unit)? = null

    @Synchronized
    fun register(canceler: (String) -> Unit) {
        this.canceler = canceler
    }

    @Synchronized
    fun unregister(canceler: (String) -> Unit) {
        if (this.canceler === canceler) {
            this.canceler = null
        }
    }

    fun cancel(notificationKey: String) {
        val activeCanceler = synchronized(this) { canceler }
        activeCanceler?.invoke(notificationKey)
        PendingIntentRegistry.remove(notificationKey)
    }
}
