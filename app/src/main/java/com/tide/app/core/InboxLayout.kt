package com.tide.app.core

import com.tide.app.data.DeliveryMode
import com.tide.app.data.InboxBatch
import com.tide.app.data.NotificationEntity

data class InboxSections(
    val drop: InboxBatch?,
    val dropUpcoming: Boolean,
    val held: List<NotificationEntity>,
    val older: List<NotificationEntity>,
)

/**
 * Splits captured notifications into Tide drop, Held, and Older.
 *
 * Tide drop is the next scheduled delivery, or the batch that just released if
 * nothing is queued yet. Held is everything else still waiting (later drops,
 * unbatched). Older is delivered Instant history plus past drops.
 * The three lists never share a notification.
 */
object InboxLayout {
    const val UNBATCHED_BATCH_ID = "unbatched"

    fun partition(
        batches: List<InboxBatch>,
        notifications: List<NotificationEntity>,
        nowMillis: Long,
    ): InboxSections {
        val waiting = batches
            .filter { it.batchId != UNBATCHED_BATCH_ID && it.releaseAtMillis > nowMillis }
            .sortedBy { it.releaseAtMillis }
        val released = batches
            .filter { it.batchId != UNBATCHED_BATCH_ID && it.releaseAtMillis in 1..nowMillis }
            .sortedByDescending { it.releaseAtMillis }

        val drop = waiting.firstOrNull() ?: released.firstOrNull()
        val dropUpcoming = drop != null && drop.releaseAtMillis > nowMillis
        val dropKeys = drop?.notifications?.map { it.notificationKey }?.toSet().orEmpty()

        val heldKeys = linkedSetOf<String>()
        val held = mutableListOf<NotificationEntity>()
        fun addHeld(items: List<NotificationEntity>) {
            items.forEach { item ->
                if (item.notificationKey in dropKeys) return@forEach
                if (item.isArchived) return@forEach
                if (item.deliveryMode != DeliveryMode.BATCH) return@forEach
                if (!heldKeys.add(item.notificationKey)) return@forEach
                held += item
            }
        }

        val laterWaiting = if (dropUpcoming) waiting.drop(1) else waiting
        laterWaiting.forEach { addHeld(it.notifications) }
        batches.filter { it.batchId == UNBATCHED_BATCH_ID }.forEach { addHeld(it.notifications) }
        addHeld(
            notifications.filter { item ->
                item.deliveryMode == DeliveryMode.BATCH && (item.batchId == null || item.batchId == UNBATCHED_BATCH_ID)
            },
        )

        val older = notifications
            .filter { item -> item.notificationKey !in dropKeys && item.notificationKey !in heldKeys }
            .sortedByDescending { it.postedAtMillis }

        return InboxSections(
            drop = drop,
            dropUpcoming = dropUpcoming,
            held = held.sortedByDescending { it.postedAtMillis },
            older = older,
        )
    }
}
