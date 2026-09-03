package com.tide.app.core

import com.tide.app.data.DeliveryMode
import com.tide.app.data.NotificationEntity
import com.tide.app.data.RuleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationGroupingTest {
    @Test
    fun dropKeepsSeparateWhatsAppThreadsAsSeparateRows() {
        val sng = whatsapp(
            key = "0|com.whatsapp|123|sng",
            title = "SnG: Ahmar Jamal",
            text = "Also gpt coming today finally",
            posted = 2_000,
        )
        val yzv = whatsapp(
            key = "0|com.whatsapp|123|yzv",
            title = "YZV Çakışan Dersler",
            text = "Merhaba arkadaşlar",
            posted = 1_000,
        )
        val reddit = notification(
            key = "0|com.reddit|9|post",
            packageName = "com.reddit.frontpage",
            appLabel = "Reddit",
            title = "r/android",
            text = "A post",
            posted = 1_500,
        )

        val rows = NotificationGrouping.rows(listOf(sng, yzv, reddit))

        assertEquals(listOf(sng.notificationKey, reddit.notificationKey, yzv.notificationKey), rows.map { it.notificationKey })
        assertEquals(sng.notificationKey, NotificationGrouping.rowKey(sng))
        assertNotEquals(NotificationGrouping.rowKey(sng), NotificationGrouping.rowKey(yzv))
    }

    @Test
    fun sameAppTitleAndChannelStillStaySeparate() {
        val first = whatsapp(key = "thread-a", title = "Ana", text = "hey", posted = 20)
        val second = whatsapp(key = "thread-b", title = "Ana", text = "later", posted = 10)

        val rows = NotificationGrouping.rows(listOf(first, second))

        assertEquals(2, rows.size)
        assertEquals(listOf("thread-a", "thread-b"), rows.map { NotificationGrouping.rowKey(it) })
    }

    private fun whatsapp(
        key: String,
        title: String,
        text: String,
        posted: Long,
    ): NotificationEntity = notification(
        key = key,
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = title,
        text = text,
        posted = posted,
        channelId = "chat_messages_v2",
    )

    private fun notification(
        key: String,
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        posted: Long,
        channelId: String? = "messages",
    ): NotificationEntity {
        return NotificationEntity(
            notificationKey = key,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            channelId = channelId,
            category = "msg",
            postedAtMillis = posted,
            batchId = "2026-09-03-batch-1-1020",
            deliveryMode = DeliveryMode.BATCH,
            ruleSource = RuleSource.DEFAULT,
        )
    }
}
