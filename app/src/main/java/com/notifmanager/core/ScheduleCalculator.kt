package com.notifmanager.core

import com.notifmanager.data.ScheduleRuleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ScheduledRelease(
    val schedule: ScheduleRuleEntity,
    val triggerAtMillis: Long,
    val batchId: String,
)

class ScheduleCalculator(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun nextReleaseMillis(nowMillis: Long, schedule: ScheduleRuleEntity): Long? {
        return nextRelease(nowMillis, schedule)?.triggerAtMillis
    }

    fun nextRelease(nowMillis: Long, schedule: ScheduleRuleEntity): ScheduledRelease? {
        if (!schedule.isEnabled) return null

        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val releaseTime = LocalTime.of(schedule.releaseMinutes / 60, schedule.releaseMinutes % 60)
        var release = now.toLocalDate().atTime(releaseTime).atZone(zoneId)
        while (!release.toInstant().isAfter(now.toInstant()) || !schedule.isActiveOn(release.dayOfWeek)) {
            release = release.plusDays(1)
        }
        val scheduleId = if (schedule.id > 0) schedule.id else 1
        val batchId = release.toLocalDate().toString() + "-batch-" + scheduleId + "-" + schedule.releaseMinutes
        return ScheduledRelease(schedule, release.toInstant().toEpochMilli(), batchId)
    }

    fun nextReleases(nowMillis: Long, schedules: List<ScheduleRuleEntity>): List<ScheduledRelease> {
        return schedules.mapNotNull { nextRelease(nowMillis, it) }
    }

    private fun ScheduleRuleEntity.isActiveOn(day: DayOfWeek): Boolean {
        return activeDaysMask and (1 shl (day.value - 1)) != 0
    }
}
