package com.tide.app.core

import com.tide.app.data.InstantWindowEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenHoursCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val calculator = OpenHoursCalculator(zone)

    @Test
    fun regularWindowIsOpenOnlyInsideRange() {
        val window = window(start = 9 * 60, end = 12 * 60)

        assertFalse(calculator.isOpenAt(millis(2026, 6, 22, 8), listOf(window)))
        assertTrue(calculator.isOpenAt(millis(2026, 6, 22, 10), listOf(window)))
        assertFalse(calculator.isOpenAt(millis(2026, 6, 22, 12), listOf(window)))
    }

    @Test
    fun overnightWindowUsesTheStartDaysMask() {
        val mondayOnly = window(start = 22 * 60, end = 7 * 60, activeDaysMask = 1)

        assertTrue(calculator.isOpenAt(millis(2026, 6, 22, 23), listOf(mondayOnly)))
        assertTrue(calculator.isOpenAt(millis(2026, 6, 23, 6), listOf(mondayOnly)))
        assertFalse(calculator.isOpenAt(millis(2026, 6, 23, 23), listOf(mondayOnly)))
    }

    @Test
    fun nextStartSkipsInactiveDays() {
        val mondayOnly = window(start = 9 * 60, end = 10 * 60, activeDaysMask = 1)
        val now = millis(2026, 6, 23, 12)

        assertEquals(
            millis(2026, 6, 29, 9),
            calculator.nextOpenStart(now, mondayOnly)?.triggerAtMillis,
        )
    }

    private fun window(
        start: Int,
        end: Int,
        activeDaysMask: Int = 0b1111111,
    ) = InstantWindowEntity(
        id = 1,
        startMinutes = start,
        endMinutes = end,
        activeDaysMask = activeDaysMask,
        updatedAtMillis = 0,
    )

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long {
        return LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
    }
}
