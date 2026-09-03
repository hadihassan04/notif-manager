package com.tide.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.tide.app.core.AppClassifier
import com.tide.app.core.AppRole
import com.tide.app.core.AppSelectionGroup
import com.tide.app.core.AppSignals
import com.tide.app.core.InboxLayout
import com.tide.app.core.IncomingNotification
import com.tide.app.core.Insights
import com.tide.app.core.InsightsCalculator
import com.tide.app.core.OpenHoursCalculator
import com.tide.app.core.RuleEngine
import com.tide.app.core.ScheduleCalculator
import com.tide.app.notifications.BatchScheduler
import com.tide.app.notifications.NotificationCaptureFilter
import com.tide.app.notifications.NotificationPublisher
import com.tide.app.notifications.NotificationStatusController
import com.tide.app.notifications.OpenHoursScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.ZoneId

data class InstalledApp(
    val packageName: String,
    val label: String,
    val mode: DeliveryMode,
    val isSystemApp: Boolean,
    val notificationCount: Int,
    val isRecommendedHeavyApp: Boolean,
    val isRecommendedInstantApp: Boolean = false,
    val isMediaPlayer: Boolean = false,
    val role: AppRole = AppRole.OTHER,
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
    val hasLauncherActivity: Boolean,
    val isRecommendedHeavyApp: Boolean,
    val isRecommendedInstantApp: Boolean,
    val isMediaPlayer: Boolean,
    val role: AppRole,
)

class Repository(
    private val context: Context,
    private val dao: AppDao,
    private val settings: AppSettings,
) {
    private val ruleEngine = RuleEngine()
    private val insightsCalculator = InsightsCalculator()
    private val scheduleCalculator = ScheduleCalculator()
    private val openHoursCalculator = OpenHoursCalculator()
    @Volatile private var cachedAppCatalog: List<AppCatalogEntry>? = null

    val inbox: Flow<List<InboxBatch>> = dao.observeInbox()
        .combine(dao.observeSchedules(), ::buildInboxBatches)
        .flowOn(Dispatchers.Default)

    val allNotifications: Flow<List<NotificationEntity>> = dao.observeAllNotifications()
    val recentNotifications: Flow<List<NotificationEntity>> = dao.observeRecentNotifications(RECENT_HISTORY_LIMIT)
        .flowOn(Dispatchers.IO)
    val schedules: Flow<List<ScheduleRuleEntity>> = dao.observeSchedules()
    val instantWindows: Flow<List<InstantWindowEntity>> = dao.observeInstantWindows()

    val insights: Flow<Insights> = dao.observeAllNotifications().combine(dao.observeSchedules()) { notifications, _ ->
        insightsCalculator.calculate(notifications)
    }.flowOn(Dispatchers.Default)

    val installedApps: Flow<List<InstalledApp>> = dao.observeAppRules().combine(dao.observePackageSummaries()) { rules, packageSummaries ->
        val ruleMap = rules.associateBy { it.packageName }
        loadInstalledApps(ruleMap, packageSummaries)
    }.flowOn(Dispatchers.IO)

    val rulesUi: Flow<List<AppRuleUi>> = combine(
        installedApps,
        dao.observeChannelRules(),
        dao.observeChannelSummaries(),
    ) { apps, channelRules, channelSummaries ->
        val capturedChannelsByPackage = channelSummaries.groupBy { it.packageName }
        val channelRuleMap = channelRules.associateBy { it.packageName + "\n" + it.channelId }
        apps.map { app ->
            val channels = capturedChannelsByPackage[app.packageName].orEmpty()
                .map { summary ->
                    val channelId = summary.channelId
                    val key = app.packageName + "\n" + channelId
                    val rule = channelRuleMap[key]
                    ChannelRuleUi(
                        packageName = app.packageName,
                        channelId = channelId,
                        // Captures carry the channel's current user-visible name; a
                        // rule only holds a copy from whenever it was saved.
                        channelName = summary.channelName.ifBlank { rule?.channelName ?: channelId },
                        mode = rule?.deliveryMode,
                        notificationCount = summary.notificationCount,
                    )
                }
                .sortedBy { it.channelName.lowercase() }
            AppRuleUi(
                app = app,
                channels = channels,
            )
        }
    }.flowOn(Dispatchers.Default)

    suspend fun capture(incoming: IncomingNotification): NotificationEntity {
        normalizeNonBatchableDefaults()
        normalizeMediaPlayerDefaults()
        val schedules = dao.schedules()
        val instantOverride = settings.pauseBatching.first() ||
            settings.temporaryOpenUntilMillis.first() > incoming.postedAtMillis ||
            openHoursCalculator.isOpenAt(incoming.postedAtMillis, dao.instantWindows())
        val decision = if (instantOverride) {
            null
        } else {
            ruleEngine.decide(
                incoming = incoming,
                schedules = schedules,
                appRules = dao.appRules(),
                channelRules = dao.channelRules(),
                defaultDeliveryMode = if (incoming.batchesByDefault) DeliveryMode.BATCH else DeliveryMode.INSTANT,
            )
        }
        val entity = NotificationEntity(
            notificationKey = incoming.notificationKey,
            packageName = incoming.packageName,
            appLabel = incoming.appLabel,
            title = incoming.title,
            text = incoming.text,
            channelId = incoming.channelId,
            channelName = incoming.channelName?.takeIf { it.isNotBlank() },
            category = incoming.category,
            postedAtMillis = incoming.postedAtMillis,
            batchId = decision?.batchId,
            deliveryMode = decision?.deliveryMode ?: DeliveryMode.INSTANT,
            ruleSource = decision?.ruleSource ?: RuleSource.SCHEDULE_INACTIVE,
        )
        dao.upsertNotification(entity)
        ensureAppRule(incoming.packageName, incoming.appLabel, incoming.batchesByDefault)
        return entity
    }

    suspend fun packagesMissingChannelNames(): List<String> = dao.packagesMissingChannelNames()

    suspend fun nameChannels(packageName: String, namesByChannelId: Map<String, String>) {
        namesByChannelId.forEach { (channelId, name) ->
            dao.updateChannelName(packageName, channelId, name)
            dao.updateChannelRuleName(packageName, channelId, name)
        }
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

    /**
     * Bulk mode change from the app list. Media apps can be turned off Instant;
     * playback notifications still skip the hold via the capture filter.
     */
    suspend fun setAppModes(apps: List<InstalledApp>, mode: DeliveryMode) {
        val now = System.currentTimeMillis()
        apps.forEach { app ->
            dao.upsertAppRule(
                AppRuleEntity(
                    packageName = app.packageName,
                    appLabel = app.label,
                    deliveryMode = mode,
                    updatedAtMillis = now,
                ),
            )
        }
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

    suspend fun applyPrioritySelection(apps: List<InstalledApp>, priorityPackages: Set<String>) {
        val now = System.currentTimeMillis()
        apps.filter { !it.isSystemApp }.forEach { app ->
            dao.upsertAppRule(
                AppRuleEntity(
                    packageName = app.packageName,
                    appLabel = app.label,
                    deliveryMode = if (app.packageName in priorityPackages) {
                        DeliveryMode.INSTANT
                    } else {
                        DeliveryMode.BATCH
                    },
                    updatedAtMillis = now,
                ),
            )
        }
    }

    suspend fun addSchedule(releaseMinutes: Int = 12 * 60) {
        dao.upsertSchedule(
            ScheduleRuleEntity(
                name = "",
                holdStartMinutes = 7 * 60,
                releaseMinutes = releaseMinutes.coerceIn(0, 23 * 60 + 55),
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
        reschedule()
    }

    suspend fun addInstantWindow() {
        val now = System.currentTimeMillis()
        val window = InstantWindowEntity(
            startMinutes = 17 * 60,
            endMinutes = 0,
            updatedAtMillis = now,
        )
        dao.upsertInstantWindow(window)
        if (openHoursCalculator.isOpenAt(now, listOf(window))) releaseWaitingNow(now)
        reschedule()
    }

    suspend fun updateInstantWindow(window: InstantWindowEntity) {
        val now = System.currentTimeMillis()
        val wasOpen = dao.instantWindowById(window.id)?.let {
            openHoursCalculator.isOpenAt(now, listOf(it))
        } == true
        val updated = window.copy(updatedAtMillis = now)
        dao.upsertInstantWindow(updated)
        if (!wasOpen && openHoursCalculator.isOpenAt(now, listOf(updated))) releaseWaitingNow(now)
        reschedule()
    }

    suspend fun deleteInstantWindow(id: Long) {
        dao.deleteInstantWindow(id)
        reschedule()
    }

    suspend fun startTemporaryOpen(untilMillis: Long) {
        if (untilMillis <= System.currentTimeMillis()) return
        settings.setTemporaryOpenUntilMillis(untilMillis)
        releaseWaitingNow()
    }

    suspend fun startIndefiniteOpen() {
        settings.setPauseBatching(true)
        releaseWaitingNow()
    }

    suspend fun endTemporaryOpen() {
        settings.setPauseBatching(false)
        settings.setTemporaryOpenUntilMillis(0L)
    }

    suspend fun archiveNotification(key: String) {
        dao.archiveNotification(key)
        NotificationStatusController.cancel(key)
    }

    suspend fun unarchiveNotification(key: String) = dao.unarchiveNotification(key)

    suspend fun archiveNotifications(keys: List<String>) {
        if (keys.isNotEmpty()) {
            dao.archiveNotifications(keys)
            keys.forEach(NotificationStatusController::cancel)
        }
    }

    suspend fun unarchiveNotifications(keys: List<String>) {
        if (keys.isNotEmpty()) dao.unarchiveNotifications(keys)
    }

    suspend fun archiveBatch(batchId: String) {
        val keys = if (batchId == InboxLayout.UNBATCHED_BATCH_ID) {
            dao.notificationsForUnbatchedBatch().map { it.notificationKey }
        } else {
            dao.notificationsForBatch(batchId).map { it.notificationKey }
        }
        if (batchId == InboxLayout.UNBATCHED_BATCH_ID) {
            dao.archiveUnbatchedBatch()
        } else {
            dao.archiveBatch(batchId)
        }
        keys.forEach(NotificationStatusController::cancel)
    }

    suspend fun unarchiveBatch(batchId: String) {
        if (batchId == InboxLayout.UNBATCHED_BATCH_ID) {
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

    suspend fun initialize() {
        ensureSchedules()
        normalizeNonBatchableDefaults()
        reschedule()
    }

    suspend fun reschedule() {
        val now = System.currentTimeMillis()
        val releases = scheduleCalculator.nextReleases(
            nowMillis = now,
            schedules = dao.schedules(),
        )
        val batchScheduler = BatchScheduler(context)
        releases.forEach { release ->
            batchScheduler.schedule(
                scheduleId = release.schedule.id,
                batchId = release.batchId,
                triggerAtMillis = release.triggerAtMillis,
            )
        }
        val openScheduler = OpenHoursScheduler(context)
        openHoursCalculator.nextOpenStarts(now, dao.instantWindows()).forEach { start ->
            openScheduler.schedule(start.window.id, start.triggerAtMillis)
        }
    }

    suspend fun handleOpenHoursStart(windowId: Long, triggerAtMillis: Long) {
        val now = System.currentTimeMillis()
        val window = dao.instantWindowById(windowId)
        val wasOpenBeforeBoundary = openHoursCalculator.isOpenAt(
            triggerAtMillis - 1L,
            dao.instantWindows(),
        )
        if (
            window != null &&
            !wasOpenBeforeBoundary &&
            openHoursCalculator.isOpenAt(now, listOf(window))
        ) {
            releaseWaitingNow(now)
        }
        reschedule()
    }

    suspend fun releaseWaitingNow(nowMillis: Long = System.currentTimeMillis()): Int {
        val waiting = dao.activeBatchedNotifications().filter { notification ->
            val releaseAt = notification.batchId?.let(::batchReleaseAtMillis) ?: 0L
            releaseAt == 0L || releaseAt > nowMillis
        }
        if (waiting.isEmpty()) return 0
        val releaseBatchId = openReleaseBatchId(nowMillis)
        dao.moveNotificationsToBatch(waiting.map { it.notificationKey }, releaseBatchId)
        NotificationPublisher(context).showDigest(releaseBatchId, waiting)
        return waiting.size
    }

    suspend fun deliverNotificationsNow(keys: List<String>): Int {
        if (keys.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val waiting = keys.mapNotNull { dao.notificationByKey(it) }.filter { notification ->
            !notification.isArchived && notification.deliveryMode == DeliveryMode.BATCH
        }
        if (waiting.isEmpty()) return 0
        val releaseBatchId = openReleaseBatchId(now)
        dao.moveNotificationsToBatch(waiting.map { it.notificationKey }, releaseBatchId)
        NotificationPublisher(context).showDigest(releaseBatchId, waiting)
        return waiting.size
    }

    suspend fun batchIdForSchedule(id: Long): String? {
        val schedule = dao.scheduleById(id) ?: return null
        return scheduleCalculator.nextRelease(System.currentTimeMillis(), schedule)?.batchId
    }

    suspend fun normalizeNonBatchableDefaults() {
        if (settings.nonBatchableDefaultsNormalized.first()) return
        dao.appRules()
            .filter { it.deliveryMode == DeliveryMode.BATCH }
            .forEach { rule ->
                val profile = NotificationCaptureFilter.appProfile(context.packageManager, rule.packageName)
                if (!profile.batchesByDefault) {
                    dao.upsertAppRule(
                        rule.copy(
                            deliveryMode = DeliveryMode.INSTANT,
                            updatedAtMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        settings.setNonBatchableDefaultsNormalized(true)
    }

    /** Existing installs may have media players stuck on batch delivery; flip them once. */
    suspend fun normalizeMediaPlayerDefaults() {
        if (settings.mediaPlayerDefaultsNormalized.first()) return
        dao.appRules()
            .filter {
                it.deliveryMode == DeliveryMode.BATCH &&
                    NotificationCaptureFilter.isMediaPlayerPackage(it.packageName)
            }
            .forEach { rule ->
                dao.upsertAppRule(
                    rule.copy(
                        deliveryMode = DeliveryMode.INSTANT,
                        updatedAtMillis = System.currentTimeMillis(),
                    ),
                )
            }
        settings.setMediaPlayerDefaultsNormalized(true)
    }

    private suspend fun ensureAppRule(packageName: String, appLabel: String, batchesByDefault: Boolean) {
        if (dao.appRuleFor(packageName) == null) {
            dao.upsertAppRule(
                AppRuleEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    deliveryMode = if (batchesByDefault) DeliveryMode.BATCH else DeliveryMode.INSTANT,
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun loadInstalledApps(
        ruleMap: Map<String, AppRuleEntity>,
        packageSummaries: List<NotificationPackageSummary>,
    ): List<InstalledApp> {
        val catalog = appCatalog()
        val catalogPackages = catalog.mapTo(mutableSetOf()) { it.packageName }
        val capturedApps = packageSummaries.associateBy { it.packageName }
        return catalog
            .map { entry ->
                val rule = ruleMap[entry.packageName]
                val summary = capturedApps[entry.packageName]
                InstalledApp(
                    packageName = entry.packageName,
                    label = rule?.appLabel ?: summary?.appLabel ?: entry.label,
                    mode = rule?.deliveryMode ?: AppClassifier.defaultDeliveryMode(
                        entry.role,
                        entry.isSystemApp,
                        entry.hasLauncherActivity,
                    ),
                    isSystemApp = entry.isSystemApp,
                    notificationCount = summary?.notificationCount ?: 0,
                    isRecommendedHeavyApp = entry.isRecommendedHeavyApp,
                    isRecommendedInstantApp = entry.isRecommendedInstantApp,
                    isMediaPlayer = entry.isMediaPlayer,
                    role = entry.role,
                )
            }
            .plus(
                capturedApps
                    .filterKeys { packageName -> packageName !in catalogPackages }
                    .map { (packageName, summary) ->
                        val profile = NotificationCaptureFilter.appProfile(context.packageManager, packageName)
                        InstalledApp(
                            packageName = packageName,
                            label = summary.appLabel,
                            mode = ruleMap[packageName]?.deliveryMode
                                ?: AppClassifier.defaultDeliveryMode(
                                    profile.role,
                                    profile.isSystemApp,
                                    profile.hasLauncherActivity,
                                ),
                            isSystemApp = profile.isSystemApp,
                            notificationCount = summary.notificationCount,
                            isRecommendedHeavyApp = profile.role.selectionGroup == AppSelectionGroup.EVERYTHING_ELSE &&
                                profile.role != AppRole.OTHER,
                            isRecommendedInstantApp = profile.role.selectionGroup == AppSelectionGroup.TIME_SENSITIVE,
                            isMediaPlayer = profile.isMediaPlayer,
                            role = profile.role,
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
        val smsPackages = packagesHandling(Intent(Intent.ACTION_SENDTO).setData(Uri.parse("smsto:"))) +
            packagesHandling(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
        val emailPackages = packagesHandling(Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))) +
            packagesHandling(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL))
        val dialPackages = packagesHandling(Intent(Intent.ACTION_DIAL))
        val calendarPackages = packagesHandling(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launcherApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }
        return launcherApps
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .map { info ->
                val label = info.loadLabel(pm).toString()
                val isMediaPlayer = NotificationCaptureFilter.isMediaPlayerPackage(info.packageName)
                val role = AppClassifier.classify(
                    AppSignals(
                        packageName = info.packageName,
                        label = label,
                        isMediaPlayer = isMediaPlayer,
                        androidCategory = info.category,
                        handlesSms = info.packageName in smsPackages,
                        handlesEmail = info.packageName in emailPackages,
                        handlesDial = info.packageName in dialPackages,
                        handlesCalendar = info.packageName in calendarPackages,
                    ),
                )
                AppCatalogEntry(
                    packageName = info.packageName,
                    label = label,
                    isSystemApp = info.isSystemApp(),
                    hasLauncherActivity = true,
                    isRecommendedHeavyApp = role.selectionGroup == AppSelectionGroup.EVERYTHING_ELSE &&
                        role != AppRole.OTHER,
                    isRecommendedInstantApp = role.selectionGroup == AppSelectionGroup.TIME_SENSITIVE,
                    isMediaPlayer = isMediaPlayer || role == AppRole.MEDIA,
                    role = role,
                )
            }
    }

    private fun packagesHandling(intent: Intent): Set<String> {
        val pm = context.packageManager
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    private fun buildInboxBatches(
        notifications: List<NotificationEntity>,
        @Suppress("UNUSED_PARAMETER") schedules: List<ScheduleRuleEntity>,
    ): List<InboxBatch> {
        return notifications
            .filter { it.deliveryMode == DeliveryMode.BATCH }
            .groupBy { it.batchId ?: InboxLayout.UNBATCHED_BATCH_ID }
            .map { (batchId, items) -> buildBatch(batchId, items) }
            .sortedByDescending { it.newestAtMillis }
    }

    private fun buildBatch(
        batchId: String,
        items: List<NotificationEntity>,
    ): InboxBatch {
        val sortedItems = items.sortedByDescending { it.postedAtMillis }
        val releaseMinutes = batchId.substringAfterLast("-", missingDelimiterValue = "").toIntOrNull()
        val topApps = sortedItems
            .groupingBy { it.appLabel }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        val summaryApps = topApps.joinToString()
        val releaseAtMillis = batchReleaseAtMillis(batchId)
        return InboxBatch(
            batchId = batchId,
            title = when {
                batchId == InboxLayout.UNBATCHED_BATCH_ID -> "Unbatched"
                releaseMinutes != null -> "${formatMinutes(releaseMinutes)} delivery"
                else -> "Delivery"
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

    private fun ApplicationInfo.isSystemApp(): Boolean {
        return flags and ApplicationInfo.FLAG_SYSTEM != 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
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

    private fun batchReleaseAtMillis(batchId: String): Long {
        if (batchId == InboxLayout.UNBATCHED_BATCH_ID) return 0L
        val date = runCatching { LocalDate.parse(batchId.substringBefore("-batch-")) }.getOrNull() ?: return 0L
        val releaseMinutes = batchId.substringAfterLast("-", missingDelimiterValue = "").toIntOrNull() ?: return 0L
        if (releaseMinutes !in 0 until 24 * 60) return 0L
        return date
            .atTime(releaseMinutes / 60, releaseMinutes % 60)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun openReleaseBatchId(nowMillis: Long): String {
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        val minutes = now.hour * 60 + now.minute
        return "${now.toLocalDate()}-batch-0-$minutes"
    }

    companion object {
        private const val RECENT_HISTORY_LIMIT = 600
    }
}
