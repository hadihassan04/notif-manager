package com.tide.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeDismissPolicyTest {
    @Test
    fun leftAndRightPastThresholdBothDismiss() {
        assertTrue(SwipeDismissPolicy.shouldDismiss(-SwipeDismissPolicy.THRESHOLD, 0f))
        assertTrue(SwipeDismissPolicy.shouldDismiss(SwipeDismissPolicy.THRESHOLD, 0f))
        assertTrue(SwipeDismissPolicy.shouldDismiss(-0.9f, 0f))
        assertTrue(SwipeDismissPolicy.shouldDismiss(0.9f, 0f))
    }

    @Test
    fun shortDragDoesNotDismiss() {
        assertFalse(SwipeDismissPolicy.shouldDismiss(-0.1f, 0f))
        assertFalse(SwipeDismissPolicy.shouldDismiss(0.1f, 0f))
        assertFalse(SwipeDismissPolicy.shouldDismiss(0f, 0f))
    }

    @Test
    fun flingEitherDirectionDismisses() {
        assertTrue(SwipeDismissPolicy.shouldDismiss(-0.15f, -SwipeDismissPolicy.VELOCITY))
        assertTrue(SwipeDismissPolicy.shouldDismiss(0.15f, SwipeDismissPolicy.VELOCITY))
        assertFalse(SwipeDismissPolicy.shouldDismiss(-0.05f, -SwipeDismissPolicy.VELOCITY))
        assertFalse(SwipeDismissPolicy.shouldDismiss(0.05f, SwipeDismissPolicy.VELOCITY))
    }

    @Test
    fun exitFollowsTheSwipeDirection() {
        assertEquals(-320f, SwipeDismissPolicy.exitTarget(-0.4f, 320f), 0.01f)
        assertEquals(320f, SwipeDismissPolicy.exitTarget(0.4f, 320f), 0.01f)
    }
}
