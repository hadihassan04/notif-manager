package com.tide.app.core

import com.tide.app.data.DeliveryMode
import com.tide.app.data.InboxBatch
import com.tide.app.data.NotificationEntity

data class InboxSections(
    val drop: InboxBatch?,
    val dropUpcoming: Boolean,
    val older: List<NotificationEntity>,
)

/**
 * Splits captured notifications into Tide drop and Older.
 *
 * Tide drop is the next scheduled delivery, or the batch that just released if
 * nothing is queued yet, with everything else still waiting (later drops,
 * unbatched) merged into it. Older is delivered Instant history plus past drops.
 * The two lists never share a notification.
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

        val primaryDrop = waiting.firstOrNull() ?: released.firstOrNull()
        val dropUpcoming = primaryDrop != null && primaryDrop.releaseAtMillis > nowMillis
        val dropKeys = primaryDrop?.notifications?.map { it.notificationKey }?.toSet().orEmpty()

        val waitingKeys = linkedSetOf<String>()
        val waitingItems = mutableListOf<NotificationEntity>()
        fun addWaiting(items: List<NotificationEntity>) {
            items.forEach { item ->
                if (item.notificationKey in dropKeys) return@forEach
                if (item.isArchived) return@forEach
                if (item.deliveryMode != DeliveryMode.BATCH) return@forEach
                if (!waitingKeys.add(item.notificationKey)) return@forEach
                waitingItems += item
            }
        }

        val laterWaiting = if (dropUpcoming) waiting.drop(1) else waiting
        laterWaiting.forEach { addWaiting(it.notifications) }
        batches.filter { it.batchId == UNBATCHED_BATCH_ID }.forEach { addWaiting(it.notifications) }
        addWaiting(
            notifications.filter { item ->
                item.deliveryMode == DeliveryMode.BATCH && (item.batchId == null || item.batchId == UNBATCHED_BATCH_ID)
            },
        )

        val sortedWaiting = waitingItems.sortedByDescending { it.postedAtMillis }
        val drop = when {
            primaryDrop != null && sortedWaiting.isNotEmpty() -> {
                val merged = primaryDrop.notifications + sortedWaiting
                primaryDrop.copy(
                    notifications = merged,
                    notificationCount = merged.size,
                    unreadCount = merged.count { !it.isRead },
                )
            }
            primaryDrop != null -> primaryDrop
            sortedWaiting.isNotEmpty() -> InboxBatch(
                batchId = UNBATCHED_BATCH_ID,
                title = "",
                notifications = sortedWaiting,
                releaseAtMillis = 0,
            )
            else -> null
        }

        val expandedDropKeys = dropKeys + waitingKeys
        val older = notifications
            .filter { item -> item.notificationKey !in expandedDropKeys }
            .sortedByDescending { it.postedAtMillis }

        return InboxSections(
            drop = drop,
            dropUpcoming = dropUpcoming,
            older = older,
        )
    }
}
