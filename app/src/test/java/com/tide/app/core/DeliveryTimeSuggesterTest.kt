package com.tide.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryTimeSuggesterTest {
    @Test
    fun emptyScheduleUsesMorningWhenNowIsUnknown() {
        assertEquals(8 * 60, DeliveryTimeSuggester.suggest(emptyList()))
    }

    @Test
    fun emptySchedulePicksTheNextHumanStarterFromNow() {
        assertEquals(8 * 60, DeliveryTimeSuggester.suggest(emptyList(), nowMinutes = 7 * 60))
        assertEquals(17 * 60, DeliveryTimeSuggester.suggest(emptyList(), nowMinutes = 10 * 60))
        assertEquals(21 * 60, DeliveryTimeSuggester.suggest(emptyList(), nowMinutes = 18 * 60))
        assertEquals(8 * 60, DeliveryTimeSuggester.suggest(emptyList(), nowMinutes = 22 * 60))
    }

    @Test
    fun oneSlotFillsTheOppositeSideOfTheDay() {
        val suggested = DeliveryTimeSuggester.suggest(listOf(7 * 60))
        assertEquals(19 * 60, suggested)
    }

    @Test
    fun twoSlotsFillTheOvernightGapNotNoonByDefault() {
        val suggested = DeliveryTimeSuggester.suggest(listOf(7 * 60, 17 * 60))
        assertEquals(0, suggested)
    }

    @Test
    fun threeClusteredSlotsFillTheLargestRemainingGap() {
        val suggested = DeliveryTimeSuggester.suggest(listOf(7 * 60, 17 * 60, 22 * 60))
        assertEquals(12 * 60, suggested)
    }

    @Test
    fun suggestionSnapsToHourOrHalfHour() {
        val suggested = DeliveryTimeSuggester.suggest(listOf(7 * 60 + 5))
        assertEquals(0, suggested % 30)
    }

    @Test
    fun suggestionStaysAtLeast45MinutesFromExistingSlots() {
        val existing = listOf(8 * 60, 17 * 60)
        val suggested = DeliveryTimeSuggester.suggest(existing)
        existing.forEach { slot ->
            assertTrue(
                "too close to $slot: $suggested",
                DeliveryTimeSuggester.circularDistance(suggested, slot) >= 45,
            )
        }
        assertTrue(suggested !in existing)
    }

    @Test
    fun neverReturnsAnExistingSlot() {
        val existing = listOf(8 * 60, 12 * 60, 17 * 60, 21 * 60)
        val suggested = DeliveryTimeSuggester.suggest(existing)
        assertTrue(suggested !in existing)
        assertEquals(0, suggested % 30)
    }

    @Test
    fun snapRoundsToNearestHalfHour() {
        assertEquals(12 * 60, DeliveryTimeSuggester.snap(12 * 60 + 10))
        assertEquals(12 * 60 + 30, DeliveryTimeSuggester.snap(12 * 60 + 20))
        assertEquals(13 * 60, DeliveryTimeSuggester.snap(12 * 60 + 50))
        assertEquals(0, DeliveryTimeSuggester.snap(23 * 60 + 50))
    }
}
