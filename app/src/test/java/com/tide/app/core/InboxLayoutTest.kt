package com.tide.app.core

import com.tide.app.data.DeliveryMode
import com.tide.app.data.InboxBatch
import com.tide.app.data.NotificationEntity
import com.tide.app.data.RuleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxLayoutTest {
    private val now = 1_000_000L

    @Test
    fun nextScheduledBatchIsTheTideDropAndLaterWaitingGoesToHeld() {
        val dropItem = notification("drop", batchId = "2026-06-21-batch-1-1020", posted = now - 10_000)
        val laterItem = notification("later", batchId = "2026-06-21-batch-2-1320", posted = now - 5_000)
        val drop = batch("2026-06-21-batch-1-1020", listOf(dropItem), releaseAt = now + 60_000)
        val later = batch("2026-06-21-batch-2-1320", listOf(laterItem), releaseAt = now + 120_000)

        val sections = InboxLayout.partition(listOf(drop, later), listOf(dropItem, laterItem), now)

        assertEquals("2026-06-21-batch-1-1020", sections.drop?.batchId)
        assertTrue(sections.dropUpcoming)
        assertEquals(listOf("later"), sections.held.map { it.notificationKey })
        assertTrue(sections.older.isEmpty())
    }

    @Test
    fun justReleasedDropIsShownWhenNothingIsQueued() {
        val releasedItem = notification("released", batchId = "2026-06-21-batch-1-1020", posted = now - 20_000)
        val released = batch("2026-06-21-batch-1-1020", listOf(releasedItem), releaseAt = now - 1_000)

        val sections = InboxLayout.partition(listOf(released), listOf(releasedItem), now)

        assertEquals("2026-06-21-batch-1-1020", sections.drop?.batchId)
        assertFalse(sections.dropUpcoming)
        assertTrue(sections.held.isEmpty())
        assertTrue(sections.older.isEmpty())
    }

    @Test
    fun unbatchedAndInstantHistoryStayOutOfTheDrop() {
        val dropItem = notification("drop", batchId = "2026-06-21-batch-1-1020", posted = now - 8_000)
        val heldItem = notification("held", batchId = null, posted = now - 4_000)
        val instant = notification(
            "instant",
            batchId = null,
            posted = now - 2_000,
            mode = DeliveryMode.INSTANT,
        )
        val dismissed = notification(
            "old",
            batchId = "2026-06-20-batch-1-1020",
            posted = now - 80_000,
            archived = true,
            mode = DeliveryMode.INSTANT,
        )
        val drop = batch("2026-06-21-batch-1-1020", listOf(dropItem), releaseAt = now + 30_000)

        val sections = InboxLayout.partition(
            listOf(drop),
            listOf(dropItem, heldItem, instant, dismissed),
            now,
        )

        assertEquals(listOf("drop"), sections.drop?.notifications?.map { it.notificationKey })
        assertEquals(listOf("held"), sections.held.map { it.notificationKey })
        assertEquals(listOf("instant", "old"), sections.older.map { it.notificationKey })
        assertTrue(sections.older.none { it.notificationKey == "drop" })
        assertTrue(sections.older.none { it.notificationKey == "held" })
    }

    @Test
    fun emptyInboxHasNoDrop() {
        val sections = InboxLayout.partition(emptyList(), emptyList(), now)
        assertNull(sections.drop)
        assertTrue(sections.held.isEmpty())
        assertTrue(sections.older.isEmpty())
    }

    private fun notification(
        key: String,
        batchId: String?,
        posted: Long,
        mode: DeliveryMode = DeliveryMode.BATCH,
        archived: Boolean = false,
    ): NotificationEntity {
        return NotificationEntity(
            notificationKey = key,
            packageName = "com.$key",
            appLabel = key,
            title = key,
            text = key,
            channelId = null,
            category = null,
            postedAtMillis = posted,
            batchId = batchId,
            deliveryMode = mode,
            ruleSource = RuleSource.DEFAULT,
            isArchived = archived,
        )
    }

    private fun batch(id: String, items: List<NotificationEntity>, releaseAt: Long): InboxBatch {
        return InboxBatch(
            batchId = id,
            title = id,
            notifications = items,
            releaseAtMillis = releaseAt,
        )
    }
}
