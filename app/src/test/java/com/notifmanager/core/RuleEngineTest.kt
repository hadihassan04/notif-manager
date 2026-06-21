package com.notifmanager.core

import com.notifmanager.data.AppRuleEntity
import com.notifmanager.data.ChannelRuleEntity
import com.notifmanager.data.DeliveryMode
import com.notifmanager.data.RuleSource
import com.notifmanager.data.ScheduleRuleEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val zone = ZoneId.of("UTC")
    private val engine = RuleEngine(zone)
    private val schedule = ScheduleRuleEntity(
        id = 2,
        holdStartMinutes = 14 * 60,
        releaseMinutes = 19 * 60,
        updatedAtMillis = 0,
    )

    @Test
    fun defaultRuleBatchesDuringHoldWindow() {
        val decision = engine.decide(
            incoming = incomingAt(hour = 15),
            schedules = listOf(schedule),
            appRules = emptyList(),
            channelRules = emptyList(),
        )

        assertEquals(DeliveryMode.BATCH, decision.deliveryMode)
        assertEquals(RuleSource.DEFAULT, decision.ruleSource)
        assertEquals("2026-06-21-batch-2-1140", decision.batchId)
    }

    @Test
    fun appInstantRuleWinsOverDefaultBatching() {
        val decision = engine.decide(
            incoming = incomingAt(hour = 15),
            schedules = listOf(schedule),
            appRules = listOf(
                AppRuleEntity(
                    packageName = "com.chat",
                    appLabel = "Chat",
                    deliveryMode = DeliveryMode.INSTANT,
                    updatedAtMillis = 0,
                ),
            ),
            channelRules = emptyList(),
        )

        assertEquals(DeliveryMode.INSTANT, decision.deliveryMode)
        assertEquals(RuleSource.APP, decision.ruleSource)
        assertEquals(null, decision.batchId)
    }

    @Test
    fun channelRuleWinsOverAppRule() {
        val decision = engine.decide(
            incoming = incomingAt(hour = 15, channelId = "urgent"),
            schedules = listOf(schedule),
            appRules = listOf(
                AppRuleEntity(
                    packageName = "com.chat",
                    appLabel = "Chat",
                    deliveryMode = DeliveryMode.BATCH,
                    updatedAtMillis = 0,
                ),
            ),
            channelRules = listOf(
                ChannelRuleEntity(
                    packageName = "com.chat",
                    channelId = "urgent",
                    channelName = "Urgent",
                    deliveryMode = DeliveryMode.INSTANT,
                    updatedAtMillis = 0,
                ),
            ),
        )

        assertEquals(DeliveryMode.INSTANT, decision.deliveryMode)
        assertEquals(RuleSource.CHANNEL, decision.ruleSource)
    }

    @Test
    fun explicitChannelBatchRuleCanOverrideInstantAppRule() {
        val decision = engine.decide(
            incoming = incomingAt(hour = 15, channelId = "mentions"),
            schedules = listOf(schedule),
            appRules = listOf(
                AppRuleEntity(
                    packageName = "com.chat",
                    appLabel = "Chat",
                    deliveryMode = DeliveryMode.INSTANT,
                    updatedAtMillis = 0,
                ),
            ),
            channelRules = listOf(
                ChannelRuleEntity(
                    packageName = "com.chat",
                    channelId = "mentions",
                    channelName = "Mentions",
                    deliveryMode = DeliveryMode.BATCH,
                    updatedAtMillis = 0,
                ),
            ),
        )

        assertEquals(DeliveryMode.BATCH, decision.deliveryMode)
        assertEquals(RuleSource.CHANNEL, decision.ruleSource)
        assertEquals("2026-06-21-batch-2-1140", decision.batchId)
    }

    @Test
    fun inactiveWindowAllowsInstantDelivery() {
        val decision = engine.decide(
            incoming = incomingAt(hour = 20),
            schedules = listOf(schedule),
            appRules = emptyList(),
            channelRules = emptyList(),
        )

        assertEquals(DeliveryMode.INSTANT, decision.deliveryMode)
        assertEquals(RuleSource.SCHEDULE_INACTIVE, decision.ruleSource)
    }

    @Test
    fun overnightHoldWindowSpansMidnight() {
        val overnight = schedule.copy(holdStartMinutes = 22 * 60, releaseMinutes = 7 * 60)

        assertTrue(engine.isWithinHoldWindow(incomingAt(hour = 23).postedAtMillis, overnight))
        assertTrue(engine.isWithinHoldWindow(incomingAt(day = 22, hour = 6).postedAtMillis, overnight))
        assertFalse(engine.isWithinHoldWindow(incomingAt(hour = 12).postedAtMillis, overnight))
        assertEquals("2026-06-22-batch-2-420", engine.batchIdFor(incomingAt(hour = 23).postedAtMillis, overnight))
    }

    @Test
    fun inactiveWeekdayAllowsInstantDelivery() {
        val mondayOnly = schedule.copy(activeDaysMask = 1)
        val sunday = incomingAt(day = 21, hour = 15)

        val decision = engine.decide(
            incoming = sunday,
            schedules = listOf(mondayOnly),
            appRules = emptyList(),
            channelRules = emptyList(),
        )

        assertEquals(DeliveryMode.INSTANT, decision.deliveryMode)
        assertEquals(RuleSource.SCHEDULE_INACTIVE, decision.ruleSource)
    }

    private fun incomingAt(day: Int = 21, hour: Int, channelId: String? = null): IncomingNotification {
        val millis = LocalDateTime.of(2026, 6, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
        return IncomingNotification(
            notificationKey = "key-$day-$hour",
            packageName = "com.chat",
            appLabel = "Chat",
            title = "Title",
            text = "Text",
            channelId = channelId,
            channelName = channelId,
            category = null,
            postedAtMillis = millis,
        )
    }
}
