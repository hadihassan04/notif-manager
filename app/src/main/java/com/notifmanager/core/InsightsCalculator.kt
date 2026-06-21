package com.notifmanager.core

import com.notifmanager.data.DeliveryMode
import com.notifmanager.data.NotificationEntity
import java.time.Instant
import java.time.ZoneId

data class AppInsight(
    val packageName: String,
    val appLabel: String,
    val received: Int,
    val batched: Int,
    val instant: Int,
)

data class HourInsight(
    val hour: Int,
    val count: Int,
)

data class DayInsight(
    val dayLabel: String,
    val count: Int,
)

data class Insights(
    val received: Int,
    val batched: Int,
    val instant: Int,
    val distractionsSaved: Int,
    val topApps: List<AppInsight>,
    val busiestHours: List<HourInsight>,
    val weeklyTrend: List<DayInsight>,
)

class InsightsCalculator(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun calculate(notifications: List<NotificationEntity>, nowMillis: Long = System.currentTimeMillis()): Insights {
        val visible = notifications.filterNot { it.isArchived }
        val batched = visible.count { it.deliveryMode == DeliveryMode.BATCH }
        val instant = visible.count { it.deliveryMode == DeliveryMode.INSTANT }
        val topApps = visible
            .groupBy { it.packageName }
            .map { (_, items) ->
                val first = items.maxBy { it.postedAtMillis }
                AppInsight(
                    packageName = first.packageName,
                    appLabel = first.appLabel,
                    received = items.size,
                    batched = items.count { it.deliveryMode == DeliveryMode.BATCH },
                    instant = items.count { it.deliveryMode == DeliveryMode.INSTANT },
                )
            }
            .sortedWith(compareByDescending<AppInsight> { it.received }.thenBy { it.appLabel })

        val busiestHours = visible
            .groupingBy {
                Instant.ofEpochMilli(it.postedAtMillis).atZone(zoneId).hour
            }
            .eachCount()
            .map { HourInsight(hour = it.key, count = it.value) }
            .sortedByDescending { it.count }
            .take(4)

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val weeklyTrend = (6 downTo 0).map { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            val count = visible.count {
                Instant.ofEpochMilli(it.postedAtMillis).atZone(zoneId).toLocalDate() == day
            }
            DayInsight(dayLabel = day.dayOfWeek.name.take(3), count = count)
        }

        return Insights(
            received = visible.size,
            batched = batched,
            instant = instant,
            distractionsSaved = batched,
            topApps = topApps,
            busiestHours = busiestHours,
            weeklyTrend = weeklyTrend,
        )
    }
}
