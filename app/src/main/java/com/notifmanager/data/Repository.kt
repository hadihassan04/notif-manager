package com.notifmanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.notifmanager.core.IncomingNotification
import com.notifmanager.core.Insights
import com.notifmanager.core.InsightsCalculator
import com.notifmanager.core.RuleEngine
import com.notifmanager.core.ScheduleCalculator
import com.notifmanager.notifications.BatchScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class InstalledApp(
    val packageName: String,
    val label: String,
    val mode: DeliveryMode,
    val isSystemApp: Boolean,
    val notificationCount: Int,
    val isRecommendedHeavyApp: Boolean,
)

data class InboxBatch(
    val batchId: String,
    val title: String,
    val notifications: List<NotificationEntity>,
    val topApps: List<String> = emptyList(),
    val newestAtMillis: Long = 0,
    val notificationCount: Int = notifications.size,
    val unreadCount: Int = notifications.count { !it.isRead },
    val summaryText: String = "",
    val releaseLabel: String = "",
)

data class ChannelRuleUi(
    val packageName: String,
    val channelId: String,
    val channelName: String,
    val mode: DeliveryMode?,
    val notificationCount: Int,
)

data class AppRuleUi(
    val app: InstalledApp,
    val channels: List<ChannelRuleUi>,
)

class Repository(
    private val context: Context,
    private val dao: AppDao,
) {
    private val ruleEngine = RuleEngine()
    private val insightsCalculator = InsightsCalculator()
    private val scheduleCalculator = ScheduleCalculator()

    val inbox: Flow<List<InboxBatch>> = dao.observeInbox().combine(dao.observeSchedules(), ::buildInboxBatches)

    val allNotifications: Flow<List<NotificationEntity>> = dao.observeAllNotifications()
    val schedules: Flow<List<ScheduleRuleEntity>> = dao.observeSchedules()

    val insights: Flow<Insights> = dao.observeAllNotifications().combine(dao.observeSchedules()) { notifications, _ ->
        insightsCalculator.calculate(notifications)
    }

    val installedApps: Flow<List<InstalledApp>> = dao.observeAppRules().combine(dao.observeAllNotifications()) { rules, notifications ->
        val ruleMap = rules.associateBy { it.packageName }
        val notificationsByPackage = notifications.groupBy { it.packageName }
        val capturedApps = notificationsByPackage
            .mapValues { it.value.maxBy { item -> item.postedAtMillis }.appLabel }
        val notificationCounts = notificationsByPackage.mapValues { it.value.size }
        loadInstalledApps(ruleMap, capturedApps, notificationCounts)
    }

    val rulesUi: Flow<List<AppRuleUi>> = combine(
        installedApps,
        dao.observeChannelRules(),
        dao.observeAllNotifications(),
    ) { apps, channelRules, notifications ->
        val capturedChannels = notifications
            .filter { it.channelId != null }
            .groupBy { it.packageName + "\n" + it.channelId }
        val channelRuleMap = channelRules.associateBy { it.packageName + "\n" + it.channelId }
        apps.map { app ->
            AppRuleUi(
                app = app,
                channels = capturedChannels
                    .filterKeys { it.substringBefore("\n") == app.packageName }
                    .map { (key, items) ->
                        val channelId = key.substringAfter("\n")
                        val latest = items.maxBy { it.postedAtMillis }
                        val rule = channelRuleMap[key]
                        ChannelRuleUi(
                            packageName = app.packageName,
                            channelId = channelId,
                            channelName = rule?.channelName ?: latest.channelId ?: channelId,
                            mode = rule?.deliveryMode,
                            notificationCount = items.size,
                        )
                    }
                    .sortedBy { it.channelName.lowercase() },
            )
        }
    }

    fun batch(batchId: String): Flow<InboxBatch?> {
        return dao.observeBatch(batchId).combine(dao.observeSchedules()) { notifications, schedules ->
            buildInboxBatches(notifications, schedules).firstOrNull { it.batchId == batchId }
                ?: notifications.takeIf { it.isNotEmpty() }?.let { items ->
                    buildBatch(batchId, items, schedules.associateBy { it.id })
                }
        }
    }

    suspend fun capture(incoming: IncomingNotification): NotificationEntity {
        val schedules = ensureSchedules()
        val decision = ruleEngine.decide(
            incoming = incoming,
            schedules = schedules,
            appRules = dao.appRules(),
            channelRules = dao.channelRules(),
        )
        val entity = NotificationEntity(
            notificationKey = incoming.notificationKey,
            packageName = incoming.packageName,
            appLabel = incoming.appLabel,
            title = incoming.title,
            text = incoming.text,
            channelId = incoming.channelId,
            category = incoming.category,
            postedAtMillis = incoming.postedAtMillis,
            batchId = decision.batchId,
            deliveryMode = decision.deliveryMode,
            ruleSource = decision.ruleSource,
        )
        dao.upsertNotification(entity)
        ensureAppRule(incoming.packageName, incoming.appLabel)
        return entity
    }

    suspend fun setAppMode(packageName: String, appLabel: String, mode: DeliveryMode) {
        dao.upsertAppRule(
            AppRuleEntity(
                packageName = packageName,
                appLabel = appLabel,
                deliveryMode = mode,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setChannelMode(
        packageName: String,
        channelId: String,
        channelName: String,
        mode: DeliveryMode,
    ) {
        dao.upsertChannelRule(
            ChannelRuleEntity(
                packageName = packageName,
                channelId = channelId,
                channelName = channelName,
                deliveryMode = mode,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun resetChannelMode(packageName: String, channelId: String) {
        dao.deleteChannelRule(packageName, channelId)
    }

    suspend fun addSchedule() {
        val existing = ensureSchedules()
        val nextNumber = existing.size + 1
        dao.upsertSchedule(
            defaultSchedule().copy(
                name = "Batch $nextNumber",
                holdStartMinutes = ((9 + nextNumber) % 24) * 60,
                releaseMinutes = ((12 + nextNumber) % 24) * 60,
            ),
        )
        reschedule()
    }

    suspend fun updateSchedule(schedule: ScheduleRuleEntity) {
        dao.upsertSchedule(schedule.copy(updatedAtMillis = System.currentTimeMillis()))
        reschedule()
    }

    suspend fun deleteSchedule(id: Long) {
        dao.deleteSchedule(id)
        ensureSchedules()
        reschedule()
    }

    suspend fun archiveNotification(key: String) = dao.archiveNotification(key)

    suspend fun unarchiveNotification(key: String) = dao.unarchiveNotification(key)

    suspend fun archiveBatch(batchId: String) = dao.archiveBatch(batchId)

    suspend fun unarchiveBatch(batchId: String) = dao.unarchiveBatch(batchId)

    suspend fun markNotificationRead(key: String) = dao.markNotificationRead(key)

    suspend fun markBatchRead(batchId: String) = dao.markBatchRead(batchId)

    suspend fun notificationsForBatch(batchId: String): List<NotificationEntity> = dao.notificationsForBatch(batchId)

    suspend fun cleanupHistory(retentionDays: Int) {
        if (retentionDays <= 0) return
        val cutoffMillis = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        dao.deleteHistoryOlderThan(cutoffMillis)
    }

    suspend fun reschedule() {
        val releases = scheduleCalculator.nextReleases(
            nowMillis = System.currentTimeMillis(),
            schedules = ensureSchedules(),
        )
        val scheduler = BatchScheduler(context)
        releases.forEach { release ->
            scheduler.schedule(
                scheduleId = release.schedule.id,
                batchId = release.batchId,
                triggerAtMillis = release.triggerAtMillis,
            )
        }
    }

    suspend fun batchIdForSchedule(id: Long): String? {
        val schedule = dao.scheduleById(id) ?: return null
        return scheduleCalculator.nextRelease(System.currentTimeMillis(), schedule)?.batchId
    }

    private suspend fun ensureAppRule(packageName: String, appLabel: String) {
        if (dao.appRuleFor(packageName) == null) {
            dao.upsertAppRule(
                AppRuleEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    deliveryMode = DeliveryMode.BATCH,
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun loadInstalledApps(
        ruleMap: Map<String, AppRuleEntity>,
        capturedApps: Map<String, String>,
        notificationCounts: Map<String, Int>,
    ): List<InstalledApp> {
        val pm = context.packageManager
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        return installed
            .map {
                val label = it.loadLabel(pm).toString()
                val rule = ruleMap[it.packageName]
                InstalledApp(
                    packageName = it.packageName,
                    label = rule?.appLabel ?: capturedApps[it.packageName] ?: label,
                    mode = rule?.deliveryMode ?: DeliveryMode.BATCH,
                    isSystemApp = it.isSystemApp(),
                    notificationCount = notificationCounts[it.packageName] ?: 0,
                    isRecommendedHeavyApp = isRecommendedHeavyApp(it.packageName, label),
                )
            }
            .plus(
                capturedApps
                    .filterKeys { packageName -> installed.none { it.packageName == packageName } }
                    .map { (packageName, label) ->
                        InstalledApp(
                            packageName = packageName,
                            label = label,
                            mode = ruleMap[packageName]?.deliveryMode ?: DeliveryMode.BATCH,
                            isSystemApp = false,
                            notificationCount = notificationCounts[packageName] ?: 0,
                            isRecommendedHeavyApp = false,
                        )
                    },
            )
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<InstalledApp> { it.notificationCount > 0 || it.isRecommendedHeavyApp }
                    .thenByDescending { it.notificationCount }
                    .thenByDescending { it.isRecommendedHeavyApp }
                    .thenBy { it.label.lowercase() },
            )
    }

    private fun buildInboxBatches(
        notifications: List<NotificationEntity>,
        schedules: List<ScheduleRuleEntity>,
    ): List<InboxBatch> {
        val schedulesById = schedules.associateBy { it.id }
        return notifications
            .filter { it.deliveryMode == DeliveryMode.BATCH }
            .groupBy { it.batchId ?: "unbatched" }
            .map { (batchId, items) -> buildBatch(batchId, items, schedulesById) }
            .sortedByDescending { it.newestAtMillis }
    }

    private fun buildBatch(
        batchId: String,
        items: List<NotificationEntity>,
        schedulesById: Map<Long, ScheduleRuleEntity>,
    ): InboxBatch {
        val sortedItems = items.sortedByDescending { it.postedAtMillis }
        val scheduleName = scheduleIdFromBatchId(batchId)?.let { schedulesById[it]?.name }
        val date = batchId.substringBefore("-batch-")
        val releaseMinutes = batchId.substringAfterLast("-", missingDelimiterValue = "").toIntOrNull()
        val topApps = sortedItems
            .groupingBy { it.appLabel }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        val summaryApps = topApps.joinToString()
        return InboxBatch(
            batchId = batchId,
            title = when {
                batchId == "unbatched" -> "Unbatched"
                scheduleName != null -> scheduleName + " · " + date
                else -> "Batch " + date
            },
            notifications = sortedItems,
            topApps = topApps,
            newestAtMillis = sortedItems.maxOfOrNull { it.postedAtMillis } ?: 0L,
            notificationCount = sortedItems.size,
            unreadCount = sortedItems.count { !it.isRead },
            summaryText = when {
                summaryApps.isBlank() -> "${sortedItems.size} held notifications"
                sortedItems.size == 1 -> "1 held from $summaryApps"
                else -> "${sortedItems.size} held from $summaryApps"
            },
            releaseLabel = releaseMinutes?.let { "Digest at ${formatMinutes(it)}" } ?: "Digest pending",
        )
    }

    private fun defaultSchedule(): ScheduleRuleEntity {
        return ScheduleRuleEntity(updatedAtMillis = System.currentTimeMillis())
    }

    private suspend fun ensureSchedules(): List<ScheduleRuleEntity> {
        val existing = dao.schedules()
        if (existing.isNotEmpty()) return existing
        val id = dao.upsertSchedule(defaultSchedule())
        return listOf(defaultSchedule().copy(id = id))
    }

    private fun scheduleIdFromBatchId(batchId: String): Long? {
        return batchId.substringAfter("-batch-", missingDelimiterValue = "")
            .substringBefore("-")
            .toLongOrNull()
    }

    private fun ApplicationInfo.isSystemApp(): Boolean {
        return flags and ApplicationInfo.FLAG_SYSTEM != 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    private fun isRecommendedHeavyApp(packageName: String, label: String): Boolean {
        val haystack = (packageName + " " + label).lowercase()
        return RECOMMENDED_HEAVY_APP_HINTS.any { haystack.contains(it) }
    }

    private fun formatMinutes(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        val suffix = if (hour >= 12) "PM" else "AM"
        val hour12 = when (val value = hour % 12) {
            0 -> 12
            else -> value
        }
        return "$hour12:${"%02d".format(minute)} $suffix"
    }

    companion object {
        private val RECOMMENDED_HEAVY_APP_HINTS = listOf(
            "instagram",
            "facebook",
            "linkedin",
            "gmail",
            "whatsapp",
            "telegram",
            "messenger",
            "slack",
            "discord",
            "twitter",
            "x.android",
            "snapchat",
            "tiktok",
            "reddit",
            "youtube",
            "outlook",
        )
    }
}
