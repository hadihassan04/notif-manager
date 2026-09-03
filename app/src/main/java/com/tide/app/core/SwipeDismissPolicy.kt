package com.tide.app.core

/**
 * Inbox swipe is dismiss in both directions. Deliver-now stays a separate
 * explicit action, not a right-swipe.
 */
object SwipeDismissPolicy {
    const val THRESHOLD = 0.32f
    const val VELOCITY = 1200f

    fun shouldDismiss(fraction: Float, velocity: Float): Boolean {
        val pastThreshold = kotlin.math.abs(fraction) >= THRESHOLD
        val fling = kotlin.math.abs(velocity) >= VELOCITY && kotlin.math.abs(fraction) > 0.1f
        return pastThreshold || fling
    }

    fun exitTarget(fraction: Float, width: Float): Float {
        return if (fraction >= 0f) width else -width
    }
}
