package com.notifmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM notifications WHERE isArchived = 0 ORDER BY postedAtMillis DESC")
    fun observeInbox(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY postedAtMillis DESC")
    fun observeAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE batchId = :batchId ORDER BY postedAtMillis DESC")
    fun observeBatch(batchId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE batchId = :batchId AND isArchived = 0 ORDER BY postedAtMillis DESC")
    suspend fun notificationsForBatch(batchId: String): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE notificationKey = :key LIMIT 1")
    suspend fun notificationByKey(key: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isArchived = 1 WHERE notificationKey = :key")
    suspend fun archiveNotification(key: String)

    @Query("UPDATE notifications SET isArchived = 0 WHERE notificationKey = :key")
    suspend fun unarchiveNotification(key: String)

    @Query("UPDATE notifications SET isArchived = 1 WHERE batchId = :batchId")
    suspend fun archiveBatch(batchId: String)

    @Query("UPDATE notifications SET isArchived = 0 WHERE batchId = :batchId")
    suspend fun unarchiveBatch(batchId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE notificationKey = :key")
    suspend fun markNotificationRead(key: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE batchId = :batchId")
    suspend fun markBatchRead(batchId: String)

    @Query(
        """
        DELETE FROM notifications
        WHERE postedAtMillis < :cutoffMillis
        AND NOT (isArchived = 0 AND deliveryMode = 'BATCH' AND batchId IS NOT NULL)
        """,
    )
    suspend fun deleteHistoryOlderThan(cutoffMillis: Long)

    @Query("SELECT * FROM app_rules ORDER BY appLabel COLLATE NOCASE")
    fun observeAppRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    suspend fun appRules(): List<AppRuleEntity>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun appRuleFor(packageName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRule(rule: AppRuleEntity)

    @Query("SELECT * FROM channel_rules")
    suspend fun channelRules(): List<ChannelRuleEntity>

    @Query("SELECT * FROM channel_rules ORDER BY packageName COLLATE NOCASE, channelName COLLATE NOCASE")
    fun observeChannelRules(): Flow<List<ChannelRuleEntity>>

    @Query("SELECT * FROM channel_rules WHERE packageName = :packageName AND channelId = :channelId LIMIT 1")
    suspend fun channelRuleFor(packageName: String, channelId: String): ChannelRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannelRule(rule: ChannelRuleEntity)

    @Query("DELETE FROM channel_rules WHERE packageName = :packageName AND channelId = :channelId")
    suspend fun deleteChannelRule(packageName: String, channelId: String)

    @Query("SELECT * FROM schedule_rules ORDER BY releaseMinutes, holdStartMinutes")
    fun observeSchedules(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_rules ORDER BY releaseMinutes, holdStartMinutes")
    suspend fun schedules(): List<ScheduleRuleEntity>

    @Query("SELECT * FROM schedule_rules WHERE id = :id LIMIT 1")
    suspend fun scheduleById(id: Long): ScheduleRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(rule: ScheduleRuleEntity): Long

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("SELECT * FROM instant_windows ORDER BY startMinutes, endMinutes")
    fun observeInstantWindows(): Flow<List<InstantWindowEntity>>

    @Query("SELECT * FROM instant_windows ORDER BY startMinutes, endMinutes")
    suspend fun instantWindows(): List<InstantWindowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstantWindow(window: InstantWindowEntity): Long

    @Query("DELETE FROM instant_windows WHERE id = :id")
    suspend fun deleteInstantWindow(id: Long)
}
