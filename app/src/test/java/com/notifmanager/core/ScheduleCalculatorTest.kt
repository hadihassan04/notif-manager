package com.notifmanager.core

import com.notifmanager.data.ScheduleRuleEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val calculator = ScheduleCalculator(zone)

    @Test
    fun nextReleaseUsesTodayWhenFuture() {
        val now = millis(2026, 6, 21, 10, 0)
        val schedule = ScheduleRuleEntity(id = 4, releaseMinutes = 19 * 60, updatedAtMillis = 0)

        assertEquals(millis(2026, 6, 21, 19, 0), calculator.nextReleaseMillis(now, schedule))
        assertEquals("2026-06-21-batch-4-1140", calculator.nextRelease(now, schedule)?.batchId)
    }

    @Test
    fun nextReleaseUsesTomorrowWhenAlreadyPassed() {
        val now = millis(2026, 6, 21, 20, 0)
        val schedule = ScheduleRuleEntity(releaseMinutes = 19 * 60, updatedAtMillis = 0)

        assertEquals(millis(2026, 6, 22, 19, 0), calculator.nextReleaseMillis(now, schedule))
    }

    @Test
    fun disabledScheduleDoesNotSchedule() {
        val now = millis(2026, 6, 21, 10, 0)
        val schedule = ScheduleRuleEntity(isEnabled = false, updatedAtMillis = 0)

        assertNull(calculator.nextReleaseMillis(now, schedule))
    }

    @Test
    fun nextReleaseSkipsInactiveDays() {
        val now = millis(2026, 6, 21, 10, 0)
        val mondayOnly = ScheduleRuleEntity(
            releaseMinutes = 19 * 60,
            activeDaysMask = 1,
            updatedAtMillis = 0,
        )

        assertEquals(millis(2026, 6, 22, 19, 0), calculator.nextReleaseMillis(now, mondayOnly))
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
    }
}
