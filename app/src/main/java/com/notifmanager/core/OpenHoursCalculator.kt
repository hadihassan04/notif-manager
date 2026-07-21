package com.notifmanager.core

import com.notifmanager.data.InstantWindowEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class ScheduledOpenStart(
    val window: InstantWindowEntity,
    val triggerAtMillis: Long,
)

class OpenHoursCalculator(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun isOpenAt(epochMillis: Long, windows: List<InstantWindowEntity>): Boolean {
        val now = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val minute = now.toLocalTime().toSecondOfDay() / 60
        return windows.any { window ->
            if (!window.isEnabled || window.startMinutes == window.endMinutes) {
                false
            } else if (window.startMinutes < window.endMinutes) {
                window.isActiveOn(now.dayOfWeek) && minute in window.startMinutes until window.endMinutes
            } else if (minute >= window.startMinutes) {
                window.isActiveOn(now.dayOfWeek)
            } else {
                minute < window.endMinutes && window.isActiveOn(now.minusDays(1).dayOfWeek)
            }
        }
    }

    fun nextOpenStart(nowMillis: Long, window: InstantWindowEntity): ScheduledOpenStart? {
        if (!window.isEnabled || window.startMinutes == window.endMinutes) return null
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val startTime = java.time.LocalTime.of(window.startMinutes / 60, window.startMinutes % 60)
        repeat(8) { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = date.atTime(startTime).atZone(zoneId)
            if (window.isActiveOn(candidate.dayOfWeek) && candidate.toInstant().isAfter(now.toInstant())) {
                return ScheduledOpenStart(window, candidate.toInstant().toEpochMilli())
            }
        }
        return null
    }

    fun nextOpenStarts(nowMillis: Long, windows: List<InstantWindowEntity>): List<ScheduledOpenStart> {
        return windows.mapNotNull { nextOpenStart(nowMillis, it) }
    }

    private fun InstantWindowEntity.isActiveOn(day: DayOfWeek): Boolean {
        return activeDaysMask and (1 shl (day.value - 1)) != 0
    }
}
