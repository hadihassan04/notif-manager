package com.notifmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DeliveryMode {
    BATCH,
    INSTANT,
}

enum class RuleSource {
    DEFAULT,
    APP,
    CHANNEL,
    SCHEDULE_INACTIVE,
}

@Entity(
    tableName = "notifications",
    indices = [
        Index("packageName"),
        Index("postedAtMillis"),
        Index("batchId"),
    ],
)
data class NotificationEntity(
    @PrimaryKey val notificationKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val channelId: String?,
    val category: String?,
    val postedAtMillis: Long,
    val batchId: String?,
    val deliveryMode: DeliveryMode,
    val ruleSource: RuleSource,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "app_rules",
    indices = [Index(value = ["packageName"], unique = true)],
)
data class AppRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val deliveryMode: DeliveryMode = DeliveryMode.BATCH,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "channel_rules",
    indices = [Index(value = ["packageName", "channelId"], unique = true)],
)
data class ChannelRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val channelId: String,
    val channelName: String,
    val deliveryMode: DeliveryMode = DeliveryMode.BATCH,
    val updatedAtMillis: Long,
)

@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Evening batch",
    val isEnabled: Boolean = true,
    val holdStartMinutes: Int = 14 * 60,
    val releaseMinutes: Int = 19 * 60,
    val activeDaysMask: Int = ALL_DAYS_MASK,
    val updatedAtMillis: Long,
) {
    companion object {
        const val ALL_DAYS_MASK = 0b1111111
    }
}
