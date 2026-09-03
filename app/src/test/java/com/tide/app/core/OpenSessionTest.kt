package com.tide.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSessionTest {
    @Test
    fun indefiniteStaysOpenUntilEnded() {
        val open = ManualOpen(indefinite = true)
        assertTrue(open.isActive(0L))
        assertTrue(open.isActive(Long.MAX_VALUE / 2))
        assertEquals(0L, open.remainingMillis(1_000L))
    }

    @Test
    fun timedOpenEndsAfterTheDeadline() {
        val open = ManualOpen(untilMillis = 5_000L)
        assertTrue(open.isActive(4_999L))
        assertFalse(open.isActive(5_000L))
        assertEquals(1_000L, open.remainingMillis(4_000L))
    }

    @Test
    fun idleIsNotOpen() {
        assertFalse(ManualOpen().isActive(1L))
    }
}
