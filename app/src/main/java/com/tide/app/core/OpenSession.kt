package com.tide.app.core

/**
 * Manual Open started from Inbox: a timed Allow-all, or Open until the user
 * ends it. Scheduled Open hours are separate and can overlap this.
 */
data class ManualOpen(
    val indefinite: Boolean = false,
    val untilMillis: Long = 0L,
) {
    fun isActive(nowMillis: Long): Boolean = indefinite || untilMillis > nowMillis

    fun remainingMillis(nowMillis: Long): Long {
        if (indefinite || untilMillis <= nowMillis) return 0L
        return untilMillis - nowMillis
    }
}
