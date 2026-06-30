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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.ZoneId

data class InstalledApp(
    val packageName: String,
    val label: String,
    val mode: DeliveryMode,
    val isSystemApp: Boolean,
    val notificationCount: Int,
    val isRecommendedHeavyApp: Boolean,
    val isRecommendedInstantApp: Boolean = false,
)

data class InboxBatch(
    val batchId: String,
    val title: String,
    val notifications: List<NotificationEntity>,
    val topApps: List<String> = emptyList(),
    val newestAtMillis: Long = 0,
    val releaseAtMillis: Long = 0,
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

private data class AppCatalogEntry(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val isRecommendedHeavyApp: Boolean,
    val isRecommendedInstantApp: Boolean,
)

class Repository(
    private val context: Context,
    private val dao: AppDao,
    private val settings: AppSettings,
) {
    private val ruleEngine = RuleEngine()
    private val insightsCalculator = InsightsCalculator()
    private val scheduleCalculator = ScheduleCalculator()
    @Volatile private var cachedAppCatalog: List<AppCatalogEntry>? = null

    val inbox: Flow<List<InboxBatch>> = dao.observeInbox()
        .combine(dao.observeSchedules(), ::buildInboxBatches)
        .flowOn(Dispatchers.Default)

    val allNotifications: Flow<List<NotificationEntity>> = dao.observeAllNotifications()
    val schedules: Flow<List<ScheduleRuleEntity>> = dao.observeSchedules()
    val instantWindows: Flow<List<InstantWindowEntity>> = dao.observeInstantWindows()

    val insights: Flow<Insights> = dao.observeAllNotifications().combine(dao.observeSchedules()) { notifications, _ ->
        insightsCalculator.calculate(notifications)
    }.flowOn(Dispatchers.Default)

    val installedApps: Flow<List<InstalledApp>> = dao.observeAppRules().combine(dao.observeAllNotifications()) { rules, notifications ->
        val ruleMap = rules.associateBy { it.packageName }
        val notificationsByPackage = notifications.groupBy { it.packageName }
        val capturedApps = notificationsByPackage
            .mapValues { it.value.maxBy { item -> item.postedAtMillis }.appLabel }
        val notificationCounts = notificationsByPackage.mapValues { it.value.size }
        loadInstalledApps(ruleMap, capturedApps, notificationCounts)
    }.flowOn(Dispatchers.IO)

    val rulesUi: Flow<List<AppRuleUi>> = combine(
        installedApps,
        dao.observeChannelRules(),
        dao.observeAllNotifications(),
    ) { apps, channelRules, notifications ->
        val capturedChannelsByPackage = notifications
            .filter { it.channelId != null }
            .groupBy { it.packageName }
        val channelRuleMap = channelRules.associateBy { it.packageName + "\n" + it.channelId }
        apps.map { app ->
            val channels = capturedChannelsByPackage[app.packageName].orEmpty()
                .groupBy { it.channelId.orEmpty() }
                .map { (channelId, items) ->
                    val latest = items.maxBy { it.postedAtMillis }
                    val key = app.packageName + "\n" + channelId
                    val rule = channelRuleMap[key]
                    ChannelRuleUi(
                        packageName = app.packageName,
                        channelId = channelId,
                        channelName = rule?.channelName ?: latest.channelId ?: channelId,
                        mode = rule?.deliveryMode,
                        notificationCount = items.size,
                    )
                }
                .sortedBy { it.channelName.lowercase() }
            AppRuleUi(
                app = app,
                channels = channels,
            )
        }
    }.flowOn(Dispatchers.Default)

    fun batch(batchId: String): Flow<InboxBatch?> {
        val notificationsFlow = if (batchId == UNBATCHED_BATCH_ID) {
            dao.observeUnbatchedBatch()
        } else {
            dao.observeBatch(batchId)
        }
        return notificationsFlow.combine(dao.observeSchedules()) { notifications, schedules ->
            val activeNotifications = notifications.filterNot { it.isArchived }
            buildInboxBatches(activeNotifications, schedules).firstOrNull { it.batchId == batchId }
                ?: activeNotifications.takeIf { it.isNotEmpty() }?.let { items ->
                    buildBatch(batchId, items, schedules.associateBy { it.id })
                }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun capture(incoming: IncomingNotification): NotificationEntity {
        val schedules = ensureSchedules()
        val instantOverride = settings.pauseBatching.first() || isInsideInstantWindow(incoming.postedAtMillis, dao.instantWindows())
        val decision = if (instantOverride) {
            null
        } else {
            ruleEngine.decide(
                incoming = incoming,
                schedules = schedules,
                appRules = dao.appRules(),
                channelRules = dao.channelRules(),
            )
        }
        val entity = NotificationEntity(
            notificationKey = incoming.notificationKey,
            packageName = incoming.packageName,
            appLabel = incoming.appLabel,
            title = incoming.title,
            text = incoming.text,
            channelId = incoming.channelId,
            category = incoming.category,
            postedAtMillis = incoming.postedAtMillis,
            batchId = decision?.batchId,
            deliveryMode = decision?.deliveryMode ?: DeliveryMode.INSTANT,
            ruleSource = decision?.ruleSource ?: RuleSource.SCHEDULE_INACTIVE,
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

    suspend fun bulkSetInstant(apps: List<InstalledApp>) {
        apps.forEach { app ->
            dao.upsertAppRule(
                AppRuleEntity(
                    packageName = app.packageName,
                    appLabel = app.label,
                    deliveryMode = DeliveryMode.INSTANT,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun addSchedule() {
        ensureSchedules()
        dao.upsertSchedule(
            ScheduleRuleEntity(
                name = "",
                holdStartMinutes = 7 * 60,
                releaseMinutes = 12 * 60,
                updatedAtMillis = System.currentTimeMillis(),
            )
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

    suspend fun addInstantWindow() {
        dao.upsertInstantWindow(
            InstantWindowEntity(
                startMinutes = 17 * 60,
                endMinutes = 0,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateInstantWindow(window: InstantWindowEntity) {
        dao.upsertInstantWindow(window.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteInstantWindow(id: Long) {
        dao.deleteInstantWindow(id)
    }

    suspend fun archiveNotification(key: String) = dao.archiveNotification(key)

    suspend fun unarchiveNotification(key: String) = dao.unarchiveNotification(key)

    suspend fun archiveNotifications(keys: List<String>) {
        if (keys.isNotEmpty()) dao.archiveNotifications(keys)
    }

    suspend fun unarchiveNotifications(keys: List<String>) {
        if (keys.isNotEmpty()) dao.unarchiveNotifications(keys)
    }

    suspend fun archiveBatch(batchId: String) {
        if (batchId == UNBATCHED_BATCH_ID) {
            dao.archiveUnbatchedBatch()
        } else {
            dao.archiveBatch(batchId)
        }
    }

    suspend fun unarchiveBatch(batchId: String) {
        if (batchId == UNBATCHED_BATCH_ID) {
            dao.unarchiveUnbatchedBatch()
        } else {
            dao.unarchiveBatch(batchId)
        }
    }

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
        val catalog = appCatalog()
        val catalogPackages = catalog.mapTo(mutableSetOf()) { it.packageName }
        return catalog
            .map { entry ->
                val rule = ruleMap[entry.packageName]
                InstalledApp(
                    packageName = entry.packageName,
                    label = rule?.appLabel ?: capturedApps[entry.packageName] ?: entry.label,
                    mode = rule?.deliveryMode ?: DeliveryMode.BATCH,
                    isSystemApp = entry.isSystemApp,
                    notificationCount = notificationCounts[entry.packageName] ?: 0,
                    isRecommendedHeavyApp = entry.isRecommendedHeavyApp,
                    isRecommendedInstantApp = entry.isRecommendedInstantApp,
                )
            }
            .plus(
                capturedApps
                    .filterKeys { packageName -> packageName !in catalogPackages }
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

    private fun appCatalog(): List<AppCatalogEntry> {
        cachedAppCatalog?.let { return it }
        return synchronized(this) {
            cachedAppCatalog ?: loadAppCatalog().also { cachedAppCatalog = it }
        }
    }

    private fun loadAppCatalog(): List<AppCatalogEntry> {
        val pm = context.packageManager
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        return installed.map {
            val label = it.loadLabel(pm).toString()
            AppCatalogEntry(
                packageName = it.packageName,
                label = label,
                isSystemApp = it.isSystemApp(),
                isRecommendedHeavyApp = isRecommendedHeavyApp(it.packageName, label),
                isRecommendedInstantApp = isRecommendedInstantApp(it.packageName, label),
            )
        }
    }

    private fun buildInboxBatches(
        notifications: List<NotificationEntity>,
        schedules: List<ScheduleRuleEntity>,
    ): List<InboxBatch> {
        val schedulesById = schedules.associateBy { it.id }
        return notifications
            .filter { it.deliveryMode == DeliveryMode.BATCH }
            .groupBy { it.batchId ?: UNBATCHED_BATCH_ID }
            .map { (batchId, items) -> buildBatch(batchId, items, schedulesById) }
            .sortedByDescending { it.newestAtMillis }
    }

    private fun buildBatch(
        batchId: String,
        items: List<NotificationEntity>,
        schedulesById: Map<Long, ScheduleRuleEntity>,
    ): InboxBatch {
        val sortedItems = items.sortedByDescending { it.postedAtMillis }
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
        val releaseAtMillis = if (releaseMinutes != null) {
            try {
                val parts = date.split("-")
                val cal = java.util.Calendar.getInstance()
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), releaseMinutes / 60, releaseMinutes % 60, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } catch (e: Exception) { 0L }
        } else 0L
        return InboxBatch(
            batchId = batchId,
            title = when {
                batchId == UNBATCHED_BATCH_ID -> "Unbatched"
                releaseMinutes != null -> "${formatMinutes(releaseMinutes)} batch"
                else -> "Batch"
            },
            notifications = sortedItems,
            topApps = topApps,
            newestAtMillis = sortedItems.maxOfOrNull { it.postedAtMillis } ?: 0L,
            releaseAtMillis = releaseAtMillis,
            notificationCount = sortedItems.size,
            unreadCount = sortedItems.count { !it.isRead },
            summaryText = when {
                summaryApps.isBlank() -> "${sortedItems.size} notifications"
                sortedItems.size == 1 -> "1 from $summaryApps"
                else -> "${sortedItems.size} from $summaryApps"
            },
            releaseLabel = releaseMinutes?.let { "Delivers at ${formatMinutes(it)}" } ?: "",
        )
    }

    private suspend fun ensureSchedules(): List<ScheduleRuleEntity> {
        val existing = dao.schedules()
        if (existing.isNotEmpty()) return existing
        listOf(
            ScheduleRuleEntity(name = "", holdStartMinutes = 22 * 60, releaseMinutes = 7 * 60, updatedAtMillis = System.currentTimeMillis()),
            ScheduleRuleEntity(name = "", holdStartMinutes = 7 * 60, releaseMinutes = 17 * 60, updatedAtMillis = System.currentTimeMillis()),
            ScheduleRuleEntity(name = "", holdStartMinutes = 17 * 60, releaseMinutes = 22 * 60, updatedAtMillis = System.currentTimeMillis()),
        ).forEach { dao.upsertSchedule(it) }
        return dao.schedules()
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

    private fun isRecommendedInstantApp(packageName: String, label: String): Boolean {
        val haystack = (packageName + " " + label).lowercase()
        return RECOMMENDED_INSTANT_HINTS.any { haystack.contains(it) }
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

    private fun isInsideInstantWindow(epochMillis: Long, windows: List<InstantWindowEntity>): Boolean {
        val minute = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .toSecondOfDay() / 60
        return windows.any { window ->
            if (!window.isEnabled || window.startMinutes == window.endMinutes) {
                false
            } else if (window.startMinutes < window.endMinutes) {
                minute in window.startMinutes until window.endMinutes
            } else {
                minute >= window.startMinutes || minute < window.endMinutes
            }
        }
    }

    companion object {
        const val UNBATCHED_BATCH_ID = "unbatched"

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

        private val RECOMMENDED_INSTANT_HINTS = listOf(
            // Communication — phone, SMS, messaging
            "com.android.phone", "com.android.dialer", "com.google.android.dialer",
            "com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging",
            "phone", "dialer", "messaging", "messages", "sms", "mms",
            // Email
            "mail", "email", "gmail", "outlook", "yahoo",
            // Banking & payments
            "bank", "chase", "wellsfargo", "citibank", "amex", "paypal", "venmo", "cashapp",
            // Navigation & rides
            "maps", "waze", "navigation", "uber", "lyft",
            // Food delivery
            "doordash", "grubhub", "ubereats", "postmates",
            // Time-sensitive system
            "calendar", "alarm", "clock",
            // Security
            "authenticator", "2fa",
        )
    }
}
