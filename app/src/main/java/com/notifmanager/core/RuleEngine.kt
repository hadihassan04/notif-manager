package com.notifmanager.core

import com.notifmanager.data.AppRuleEntity
import com.notifmanager.data.ChannelRuleEntity
import com.notifmanager.data.DeliveryMode
import com.notifmanager.data.RuleSource
import com.notifmanager.data.ScheduleRuleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class IncomingNotification(
    val notificationKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val channelId: String?,
    val channelName: String?,
    val category: String?,
    val postedAtMillis: Long,
)

data class RuleDecision(
    val deliveryMode: DeliveryMode,
    val ruleSource: RuleSource,
    val batchId: String?,
    val schedule: ScheduleRuleEntity?,
)

class RuleEngine(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun decide(
        incoming: IncomingNotification,
        schedules: List<ScheduleRuleEntity>,
        appRules: List<AppRuleEntity>,
        channelRules: List<ChannelRuleEntity>,
    ): RuleDecision {
        val schedule = schedules
            .filter { it.isEnabled }
            .sortedWith(compareBy<ScheduleRuleEntity> { it.releaseMinutes }.thenBy { it.holdStartMinutes })
            .firstOrNull { isWithinHoldWindow(incoming.postedAtMillis, it) }
        if (schedule == null) {
            return RuleDecision(DeliveryMode.INSTANT, RuleSource.SCHEDULE_INACTIVE, null, null)
        }

        val channelRule = incoming.channelId?.let { channelId ->
            channelRules.firstOrNull {
                it.packageName == incoming.packageName && it.channelId == channelId
            }
        }
        if (channelRule != null) {
            return channelRule.deliveryMode.toDecision(
                source = RuleSource.CHANNEL,
                batchId = batchIdFor(incoming.postedAtMillis, schedule),
                schedule = schedule,
            )
        }

        val appRule = appRules.firstOrNull { it.packageName == incoming.packageName }
        if (appRule != null) {
            return appRule.deliveryMode.toDecision(
                source = RuleSource.APP,
                batchId = batchIdFor(incoming.postedAtMillis, schedule),
                schedule = schedule,
            )
        }

        return RuleDecision(
            deliveryMode = DeliveryMode.BATCH,
            ruleSource = RuleSource.DEFAULT,
            batchId = batchIdFor(incoming.postedAtMillis, schedule),
            schedule = schedule,
        )
    }

    fun isWithinHoldWindow(epochMillis: Long, schedule: ScheduleRuleEntity): Boolean {
        if (!isActiveDay(epochMillis, schedule)) return false
        val minute = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime().toSecondOfDay() / 60
        val start = schedule.holdStartMinutes
        val end = schedule.releaseMinutes
        return if (start <= end) {
            minute in start until end
        } else {
            minute >= start || minute < end
        }
    }

    fun batchIdFor(epochMillis: Long, schedule: ScheduleRuleEntity): String {
        val posted = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val releaseDate = if (
            schedule.holdStartMinutes > schedule.releaseMinutes &&
            posted.toLocalTime().toSecondOfDay() / 60 >= schedule.holdStartMinutes
        ) {
            posted.toLocalDate().plusDays(1)
        } else {
            posted.toLocalDate()
        }
        val scheduleId = if (schedule.id > 0) schedule.id else 1
        return releaseDate.toString() + "-batch-" + scheduleId + "-" + schedule.releaseMinutes
    }

    fun isActiveDay(epochMillis: Long, schedule: ScheduleRuleEntity): Boolean {
        val posted = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val releaseDay = if (
            schedule.holdStartMinutes > schedule.releaseMinutes &&
            posted.toLocalTime().toSecondOfDay() / 60 >= schedule.holdStartMinutes
        ) {
            posted.plusDays(1).dayOfWeek
        } else {
            posted.dayOfWeek
        }
        return schedule.activeDaysMask and releaseDay.bit() != 0
    }

    private fun DeliveryMode.toDecision(
        source: RuleSource,
        batchId: String?,
        schedule: ScheduleRuleEntity,
    ): RuleDecision {
        return RuleDecision(
            deliveryMode = this,
            ruleSource = source,
            batchId = if (this == DeliveryMode.BATCH) batchId else null,
            schedule = if (this == DeliveryMode.BATCH) schedule else null,
        )
    }

    private fun DayOfWeek.bit(): Int = 1 shl (value - 1)
}
