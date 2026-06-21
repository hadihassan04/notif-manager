package com.notifmanager.core

import com.notifmanager.data.DeliveryMode
import com.notifmanager.data.NotificationEntity
import com.notifmanager.data.RuleSource
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val calculator = InsightsCalculator(zone)

    @Test
    fun calculatesCoreMetricsAndPerAppTotals() {
        val notifications = listOf(
            notification("1", "com.chat", "Chat", DeliveryMode.BATCH, hour = 10),
            notification("2", "com.chat", "Chat", DeliveryMode.BATCH, hour = 10),
            notification("3", "com.mail", "Mail", DeliveryMode.INSTANT, hour = 12),
            notification("4", "com.news", "News", DeliveryMode.BATCH, hour = 12, archived = true),
        )

        val insights = calculator.calculate(
            notifications,
            nowMillis = millis(2026, 6, 21, 18, 0),
        )

        assertEquals(3, insights.received)
        assertEquals(2, insights.batched)
        assertEquals(1, insights.instant)
        assertEquals(2, insights.distractionsSaved)
        assertEquals("Chat", insights.topApps.first().appLabel)
        assertEquals(2, insights.topApps.first().received)
        assertEquals(2, insights.busiestHours.first().count)
    }

    private fun notification(
        key: String,
        packageName: String,
        label: String,
        mode: DeliveryMode,
        hour: Int,
        archived: Boolean = false,
    ): NotificationEntity {
        return NotificationEntity(
            notificationKey = key,
            packageName = packageName,
            appLabel = label,
            title = "Title",
            text = "Text",
            channelId = null,
            category = null,
            postedAtMillis = millis(2026, 6, 21, hour, 0),
            batchId = if (mode == DeliveryMode.BATCH) "2026-06-21-1140" else null,
            deliveryMode = mode,
            ruleSource = RuleSource.DEFAULT,
            isArchived = archived,
        )
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
    }
}
