package com.tide.app.core

import com.tide.app.data.AppRuleEntity
import com.tide.app.data.ChannelRuleEntity
import com.tide.app.data.DeliveryMode
import com.tide.app.data.RuleSource
import com.tide.app.data.ScheduleRuleEntity
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
    val batchesByDefault: Boolean = true,
    val isMediaPlayback: Boolean = false,
)

data class RuleDecision(
    val deliveryMode: DeliveryMode,
    val ruleSource: RuleSource,
    val batchId: String?,
    val schedule: ScheduleRuleEntity?,
)

class RuleEngine(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    private val scheduleCalculator = ScheduleCalculator(zoneId)

    fun decide(
        incoming: IncomingNotification,
        schedules: List<ScheduleRuleEntity>,
        appRules: List<AppRuleEntity>,
        channelRules: List<ChannelRuleEntity>,
        defaultDeliveryMode: DeliveryMode = DeliveryMode.BATCH,
    ): RuleDecision {
        if (incoming.isMediaPlayback) {
            return RuleDecision(DeliveryMode.INSTANT, RuleSource.MEDIA_PLAYBACK, null, null)
        }
        val nextRelease = scheduleCalculator.nextReleases(incoming.postedAtMillis, schedules)
            .minByOrNull { it.triggerAtMillis }
        if (nextRelease == null) {
            return RuleDecision(DeliveryMode.INSTANT, RuleSource.SCHEDULE_INACTIVE, null, null)
        }
        val schedule = nextRelease.schedule

        val channelRule = incoming.channelId?.let { channelId ->
            channelRules.firstOrNull {
                it.packageName == incoming.packageName && it.channelId == channelId
            }
        }
        if (channelRule != null) {
            return channelRule.deliveryMode.toDecision(
                source = RuleSource.CHANNEL,
                batchId = nextRelease.batchId,
                schedule = schedule,
            )
        }

        val appRule = appRules.firstOrNull { it.packageName == incoming.packageName }
        if (appRule != null) {
            return appRule.deliveryMode.toDecision(
                source = RuleSource.APP,
                batchId = nextRelease.batchId,
                schedule = schedule,
            )
        }

        return defaultDeliveryMode.toDecision(
            source = RuleSource.DEFAULT,
            batchId = nextRelease.batchId,
            schedule = schedule,
        )
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
}
