package com.tide.app.core

/**
 * Suggests a delivery minute-of-day when the user adds a slot.
 * Existing enabled times are treated as points on a 24-hour circle.
 */
object DeliveryTimeSuggester {
    const val MINUTES_IN_DAY = 24 * 60
    const val SNAP_MINUTES = 30
    const val MIN_SEPARATION_MINUTES = 45

    /** Morning, end of work, evening — used only when the schedule is empty. */
    val HUMAN_STARTERS = listOf(8 * 60, 17 * 60, 21 * 60)

    fun suggest(existingMinutes: List<Int>, nowMinutes: Int? = null): Int {
        val existing = existingMinutes.map(::normalize).distinct().sorted()
        if (existing.isEmpty()) return nextStarter(nowMinutes)

        val gap = largestGap(existing)
        val midpoint = snap(gap.from + gap.length / 2)
        if (isUsable(midpoint, existing) && isInsideGap(midpoint, gap)) {
            return midpoint
        }

        val inGap = halfHourMarks()
            .filter { isInsideGap(it, gap) && it !in existing }
        val separated = inGap.filter { isUsable(it, existing) }
        val pool = separated.ifEmpty { inGap }
        val best = pool.maxWithOrNull(
            compareBy<Int> { minDistance(it, existing) }
                .thenBy { -circularDistance(it, midpoint) },
        )
        return best ?: midpoint
    }

    internal fun nextStarter(nowMinutes: Int?): Int {
        if (nowMinutes == null) return HUMAN_STARTERS.first()
        val now = normalize(nowMinutes)
        return HUMAN_STARTERS.minBy { starter ->
            val delta = starter - now
            if (delta > 0) delta else delta + MINUTES_IN_DAY
        }
    }

    internal fun snap(minutes: Int): Int {
        val normalized = normalize(minutes)
        val minute = normalized % 60
        val hour = normalized / 60
        val snapped = when {
            minute < 15 -> hour * 60
            minute < 45 -> hour * 60 + SNAP_MINUTES
            else -> (hour + 1) * 60
        }
        return normalize(snapped)
    }

    internal fun circularDistance(a: Int, b: Int): Int {
        val delta = kotlin.math.abs(normalize(a) - normalize(b))
        return minOf(delta, MINUTES_IN_DAY - delta)
    }

    private fun largestGap(existing: List<Int>): Gap {
        return existing.indices.map { index ->
            val from = existing[index]
            val to = existing[(index + 1) % existing.size]
            val length = if (to > from) to - from else to + MINUTES_IN_DAY - from
            Gap(from = from, to = to, length = length)
        }.maxBy { it.length }
    }

    private fun isUsable(candidate: Int, existing: List<Int>): Boolean {
        return existing.none { circularDistance(candidate, it) < MIN_SEPARATION_MINUTES }
    }

    private fun minDistance(candidate: Int, existing: List<Int>): Int {
        return existing.minOf { circularDistance(candidate, it) }
    }

    private fun isInsideGap(slot: Int, gap: Gap): Boolean {
        val point = normalize(slot)
        if (gap.length >= MINUTES_IN_DAY) return point != gap.from
        if (gap.to > gap.from) return point > gap.from && point < gap.to
        return point > gap.from || point < gap.to
    }

    private fun halfHourMarks(): List<Int> {
        return (0 until MINUTES_IN_DAY step SNAP_MINUTES).toList()
    }

    private fun normalize(minutes: Int): Int {
        val wrapped = minutes % MINUTES_IN_DAY
        return if (wrapped < 0) wrapped + MINUTES_IN_DAY else wrapped
    }

    private data class Gap(val from: Int, val to: Int, val length: Int)
}
