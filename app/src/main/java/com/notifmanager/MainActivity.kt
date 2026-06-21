package com.notifmanager

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notifmanager.core.Insights
import com.notifmanager.core.InsightsCalculator
import com.notifmanager.core.ScheduleCalculator
import com.notifmanager.data.AppRuleUi
import com.notifmanager.data.AppSettings
import com.notifmanager.data.ChannelRuleUi
import com.notifmanager.data.DeliveryMode
import com.notifmanager.data.InboxBatch
import com.notifmanager.data.InstalledApp
import com.notifmanager.data.NotificationEntity
import com.notifmanager.data.Repository
import com.notifmanager.data.ScheduleRuleEntity
import com.notifmanager.notifications.PendingIntentRegistry
import com.notifmanager.ui.theme.MdSpacing
import com.notifmanager.ui.theme.NotifManagerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.DayOfWeek
import java.util.Date
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialBatchId = intent?.getStringExtra(EXTRA_BATCH_ID)
        setContent {
            val app = application as NotifManagerApp
            val dynamicColor by app.settings.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
            NotifManagerTheme(dynamicColor = dynamicColor) {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.factory(app.repository, app.settings),
                )
                NotifManagerApp(viewModel, initialBatchId)
            }
        }
    }

    companion object {
        const val EXTRA_BATCH_ID = "batch_id"
    }
}

class MainViewModel(
    private val repository: Repository,
    private val settings: AppSettings,
) : ViewModel() {
    val inbox: StateFlow<List<InboxBatch>> = repository.inbox.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val history: StateFlow<List<NotificationEntity>> = repository.allNotifications.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val rulesUi: StateFlow<List<AppRuleUi>> = repository.rulesUi.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val schedules: StateFlow<List<ScheduleRuleEntity>> = repository.schedules.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val insights: StateFlow<Insights> = repository.insights.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InsightsCalculator().calculate(emptyList()),
    )
    val showSystemApps: StateFlow<Boolean> = settings.showSystemApps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )
    val historyRetentionDays: StateFlow<Int> = settings.historyRetentionDays.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings.DEFAULT_HISTORY_RETENTION_DAYS,
    )
    val dynamicColorEnabled: StateFlow<Boolean> = settings.dynamicColorEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true,
    )
    val onboardingCompleted: StateFlow<Boolean> = settings.onboardingCompleted.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )
    val installedApps: StateFlow<List<InstalledApp>> = repository.installedApps.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    init {
        viewModelScope.launch {
            repository.reschedule()
            repository.cleanupHistory(historyRetentionDays.value)
        }
    }

    fun batch(batchId: String): Flow<InboxBatch?> = repository.batch(batchId)

    fun setAppMode(app: InstalledApp, mode: DeliveryMode) {
        viewModelScope.launch { repository.setAppMode(app.packageName, app.label, mode) }
    }

    fun setChannelMode(channel: ChannelRuleUi, mode: DeliveryMode?) {
        viewModelScope.launch {
            if (mode == null) {
                repository.resetChannelMode(channel.packageName, channel.channelId)
            } else {
                repository.setChannelMode(channel.packageName, channel.channelId, channel.channelName, mode)
            }
        }
    }

    fun addSchedule() {
        viewModelScope.launch { repository.addSchedule() }
    }

    fun updateSchedule(schedule: ScheduleRuleEntity) {
        viewModelScope.launch { repository.updateSchedule(schedule) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }

    fun archiveBatch(batchId: String) {
        viewModelScope.launch { repository.archiveBatch(batchId) }
    }

    fun unarchiveBatch(batchId: String) {
        viewModelScope.launch { repository.unarchiveBatch(batchId) }
    }

    fun archiveNotification(key: String) {
        viewModelScope.launch { repository.archiveNotification(key) }
    }

    fun unarchiveNotification(key: String) {
        viewModelScope.launch { repository.unarchiveNotification(key) }
    }

    fun markNotificationRead(key: String) {
        viewModelScope.launch { repository.markNotificationRead(key) }
    }

    fun markBatchRead(batchId: String) {
        viewModelScope.launch { repository.markBatchRead(batchId) }
    }

    fun setShowSystemApps(enabled: Boolean) {
        viewModelScope.launch { settings.setShowSystemApps(enabled) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColorEnabled(enabled) }
    }

    fun setHistoryRetentionDays(days: Int) {
        viewModelScope.launch {
            settings.setHistoryRetentionDays(days)
            repository.cleanupHistory(days)
        }
    }

    fun archiveHistory(keys: List<String>) {
        viewModelScope.launch { keys.forEach { repository.archiveNotification(it) } }
    }

    fun cleanupHistoryNow() {
        viewModelScope.launch { repository.cleanupHistory(historyRetentionDays.value) }
    }

    fun completeOnboarding(instantApps: List<InstalledApp> = emptyList()) {
        viewModelScope.launch {
            if (instantApps.isNotEmpty()) repository.bulkSetInstant(instantApps)
            settings.setOnboardingCompleted(true)
            settings.setSetupDismissedOnce(true)
        }
    }

    fun replayOnboarding() {
        viewModelScope.launch { settings.setOnboardingCompleted(false) }
    }

    companion object {
        fun factory(repository: Repository, settings: AppSettings): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository, settings) as T
                }
            }
        }
    }
}

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Inbox("inbox", "Notifs", Icons.Filled.Inbox),
    Schedule("schedule", "Schedule", Icons.Filled.Schedule),
    Rules("rules", "Rules", Icons.Filled.Tune),
    Insights("insights", "Insights", Icons.Filled.Insights),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifManagerApp(viewModel: MainViewModel, initialBatchId: String?) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    if (!onboardingCompleted) {
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        OnboardingScreen(installedApps, viewModel::completeOnboarding)
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Destination.Inbox.route
    val topLevel = Destination.entries.firstOrNull { it.route == currentRoute }
    val title = topLevel?.label ?: if (currentRoute.startsWith("batch/")) "Batch detail" else "Notifs"

    LaunchedEffect(initialBatchId) {
        if (initialBatchId != null) {
            navController.navigate("batch/$initialBatchId")
            viewModel.markBatchRead(initialBatchId)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (topLevel == null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != Destination.Settings.route) {
                            IconButton(onClick = { navigateTopLevel(navController, Destination.Settings.route) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            },
            bottomBar = {
                if (!useRail) {
                    AppNavigationBar(navController, currentRoute)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = currentRoute == Destination.Schedule.route,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    FloatingActionButton(onClick = viewModel::addSchedule) {
                        Icon(Icons.Filled.Add, contentDescription = "Add batch")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (useRail) {
                    AppNavigationRail(navController, currentRoute)
                }
                NavHost(
                    navController = navController,
                    startDestination = Destination.Inbox.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(Destination.Inbox.route) {
                        val inbox by viewModel.inbox.collectAsStateWithLifecycle()
                        val notifications by viewModel.history.collectAsStateWithLifecycle()
                        val schedules by viewModel.schedules.collectAsStateWithLifecycle()
                        NotificationsScreen(
                            batches = inbox,
                            notifications = notifications,
                            schedules = schedules,
                            snackbarHostState = snackbarHostState,
                            onOpenBatch = { navController.navigate("batch/$it") },
                            onArchiveBatch = viewModel::archiveBatch,
                            onUnarchiveBatch = viewModel::unarchiveBatch,
                            onArchiveNotification = viewModel::archiveNotification,
                            onUnarchiveNotification = viewModel::unarchiveNotification,
                            onArchiveHistory = viewModel::archiveHistory,
                            onMarkBatchRead = viewModel::markBatchRead,
                            onMarkNotificationRead = viewModel::markNotificationRead,
                        )
                    }
                    composable(Destination.Insights.route) {
                        val insights by viewModel.insights.collectAsStateWithLifecycle()
                        InsightsScreen(insights)
                    }
                    composable(Destination.Rules.route) {
                        val rules by viewModel.rulesUi.collectAsStateWithLifecycle()
                        val showSystemApps by viewModel.showSystemApps.collectAsStateWithLifecycle()
                        RulesScreen(
                            rules = rules,
                            showSystemApps = showSystemApps,
                            onShowSystemApps = viewModel::setShowSystemApps,
                            onSetAppMode = viewModel::setAppMode,
                            onSetChannelMode = viewModel::setChannelMode,
                        )
                    }
                    composable(Destination.Schedule.route) {
                        val schedules by viewModel.schedules.collectAsStateWithLifecycle()
                        ScheduleScreen(schedules, viewModel::updateSchedule, viewModel::deleteSchedule)
                    }
                    composable(Destination.Settings.route) {
                        val showSystemApps by viewModel.showSystemApps.collectAsStateWithLifecycle()
                        val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
                        val retentionDays by viewModel.historyRetentionDays.collectAsStateWithLifecycle()
                        SettingsScreen(
                            showSystemApps = showSystemApps,
                            dynamicColor = dynamicColor,
                            retentionDays = retentionDays,
                            onShowSystemApps = viewModel::setShowSystemApps,
                            onDynamicColor = viewModel::setDynamicColorEnabled,
                            onRetentionDays = viewModel::setHistoryRetentionDays,
                            onCleanupNow = viewModel::cleanupHistoryNow,
                            onReplayOnboarding = viewModel::replayOnboarding,
                        )
                    }
                    composable(
                        route = "batch/{batchId}",
                        arguments = listOf(navArgument("batchId") { type = NavType.StringType }),
                    ) { entry ->
                        val batchId = entry.arguments?.getString("batchId").orEmpty()
                        val batch by viewModel.batch(batchId).collectAsStateWithLifecycle(initialValue = null)
                        LaunchedEffect(batchId) {
                            if (batchId.isNotBlank()) viewModel.markBatchRead(batchId)
                        }
                        BatchDetailScreen(
                            batch = batch,
                            batchId = batchId,
                            snackbarHostState = snackbarHostState,
                            onArchiveBatch = viewModel::archiveBatch,
                            onUnarchiveBatch = viewModel::unarchiveBatch,
                            onArchiveNotification = viewModel::archiveNotification,
                            onUnarchiveNotification = viewModel::unarchiveNotification,
                            onMarkNotificationRead = viewModel::markNotificationRead,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(navController: NavHostController, currentRoute: String) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Destination.entries.filter { it != Destination.Settings }.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navigateTopLevel(navController, destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(navController: NavHostController, currentRoute: String) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Spacer(Modifier.height(MdSpacing.sm))
        Destination.entries.filter { it != Destination.Settings }.forEach { destination ->
            NavigationRailItem(
                selected = currentRoute == destination.route,
                onClick = { navigateTopLevel(navController, destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Destination.Inbox.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NotificationsScreen(
    batches: List<InboxBatch>,
    notifications: List<NotificationEntity>,
    schedules: List<ScheduleRuleEntity>,
    snackbarHostState: SnackbarHostState,
    onOpenBatch: (String) -> Unit,
    onArchiveBatch: (String) -> Unit,
    onUnarchiveBatch: (String) -> Unit,
    onArchiveNotification: (String) -> Unit,
    onUnarchiveNotification: (String) -> Unit,
    onArchiveHistory: (List<String>) -> Unit,
    onMarkBatchRead: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
) {
    val expandedBatchIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var showArchived by remember { mutableStateOf(false) }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val activeBatchIds = batches.map { it.batchId }.toSet()
    val historyNotifications = notifications
        .filter { !it.isArchived && (it.batchId == null || it.batchId !in activeBatchIds) }
    val archivedNotifications = notifications.filter { it.isArchived }
    val nextBatch = batches.filter { it.releaseAtMillis > nowMillis }.minByOrNull { it.releaseAtMillis }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        if (batches.isNotEmpty() || schedules.isNotEmpty()) {
            item {
                NextBatchCard(batches = batches, schedules = schedules, nowMillis = nowMillis)
            }
        }

        if (batches.isNotEmpty()) {
            item {
                Text("Waiting", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(batches, key = { it.batchId }) { batch ->
                val expanded = batch.batchId in expandedBatchIds
                BatchSummaryCard(
                    batch = batch,
                    expanded = expanded,
                    isNext = batch.batchId == nextBatch?.batchId,
                    onToggle = {
                        if (expanded) expandedBatchIds.remove(batch.batchId) else expandedBatchIds.add(batch.batchId)
                    },
                    onOpenBatch = {
                        onMarkBatchRead(batch.batchId)
                        onOpenBatch(batch.batchId)
                    },
                    onArchiveBatch = {
                        onArchiveBatch(batch.batchId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Batch archived", "Undo")
                            if (result.name == "ActionPerformed") onUnarchiveBatch(batch.batchId)
                        }
                    },
                    onArchiveNotification = { key ->
                        onArchiveNotification(key)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Notification archived", "Undo")
                            if (result.name == "ActionPerformed") onUnarchiveNotification(key)
                        }
                    },
                    onMarkNotificationRead = onMarkNotificationRead,
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("History", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (historyNotifications.isNotEmpty()) {
                    TextButton(onClick = {
                        val keys = notifications
                            .filter { !it.isArchived && (it.batchId == null || it.batchId !in activeBatchIds) }
                            .map { it.notificationKey }
                        scope.launch {
                            onArchiveHistory(keys)
                            val result = snackbarHostState.showSnackbar("History cleared", "Undo")
                            if (result.name == "ActionPerformed") keys.forEach { onUnarchiveNotification(it) }
                        }
                    }) { Text("Clear all") }
                }
            }
        }

        if (historyNotifications.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing here yet",
                    body = "Delivered notifications will appear here.",
                )
            }
        }

        items(historyNotifications, key = { it.notificationKey }) { item ->
            NotificationRow(
                item = item,
                archiveLabel = "Archive",
                onArchive = {
                    onArchiveNotification(item.notificationKey)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar("Notification archived", "Undo")
                        if (result.name == "ActionPerformed") onUnarchiveNotification(item.notificationKey)
                    }
                },
                onRestore = null,
                onMarkRead = { onMarkNotificationRead(item.notificationKey) },
            )
        }

        if (archivedNotifications.isNotEmpty()) {
            item {
                FilledTonalButton(
                    onClick = { showArchived = !showArchived },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (showArchived) "Hide archived" else "Archived (${archivedNotifications.size})")
                }
            }
        }

        if (showArchived) {
            items(archivedNotifications, key = { "arc_${it.notificationKey}" }) { item ->
                NotificationRow(
                    item = item,
                    archiveLabel = "Restore",
                    onArchive = { onUnarchiveNotification(item.notificationKey) },
                    onRestore = { onUnarchiveNotification(item.notificationKey) },
                    onMarkRead = { onMarkNotificationRead(item.notificationKey) },
                )
            }
        }
    }
}

@Composable
private fun InboxOverviewCard(batches: List<InboxBatch>, insights: Insights) {
    val heldCount = batches.sumOf { it.notificationCount }
    val unreadCount = batches.sumOf { it.unreadCount }
    val nextDigest = batches
        .map { it.releaseLabel }
        .firstOrNull { it.isNotBlank() && it != "Digest pending" }
        ?: "No digest pending"
    val topApp = insights.topApps.firstOrNull()?.appLabel ?: "No noisy app yet"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Notification quiet zone", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$heldCount held · $unreadCount unread",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            ) {
                CompactStat("Next", nextDigest, Modifier.weight(1f))
                CompactStat("Saved", insights.distractionsSaved.toString(), Modifier.weight(1f))
                CompactStat("Top app", topApp, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NextBatchCard(batches: List<InboxBatch>, schedules: List<ScheduleRuleEntity>, nowMillis: Long) {
    val nextBatchRelease = batches.filter { it.releaseAtMillis > nowMillis }.minByOrNull { it.releaseAtMillis }
    val nextScheduleMillis = remember(schedules, nowMillis) {
        ScheduleCalculator().nextReleases(nowMillis, schedules).minByOrNull { it.triggerAtMillis }?.triggerAtMillis
    }
    val nextReleaseMillis = nextBatchRelease?.releaseAtMillis ?: nextScheduleMillis ?: return
    val remaining = nextReleaseMillis - nowMillis
    val totalHeld = batches.sumOf { it.notificationCount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Next batch",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    formatCountdown(remaining),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Held",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    "$totalHeld",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 72.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BatchSummaryCard(
    batch: InboxBatch,
    expanded: Boolean,
    isNext: Boolean = false,
    onToggle: () -> Unit,
    onOpenBatch: () -> Unit,
    onArchiveBatch: () -> Unit,
    onArchiveNotification: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                FlowerBadge(count = batch.notificationCount)
                Column(Modifier.weight(1f)) {
                    Text(batch.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        batch.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (batch.releaseLabel.isNotEmpty()) {
                        Text(
                            batch.releaseLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Toggle batch")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = onOpenBatch) {
                    Text("Open")
                }
                if (batch.unreadCount > 0) {
                    Text(
                        "${batch.unreadCount} unread",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    batch.notifications.take(8).forEach { item ->
                        NotificationRow(
                            item = item,
                            archiveLabel = "Archive",
                            onArchive = { onArchiveNotification(item.notificationKey) },
                            onRestore = null,
                            onMarkRead = { onMarkNotificationRead(item.notificationKey) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                        Button(onClick = onOpenBatch) { Text("View all") }
                        OutlinedButton(onClick = onArchiveBatch) { Text("Archive batch") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchDetailScreen(
    batch: InboxBatch?,
    batchId: String,
    snackbarHostState: SnackbarHostState,
    onArchiveBatch: (String) -> Unit,
    onUnarchiveBatch: (String) -> Unit,
    onArchiveNotification: (String) -> Unit,
    onUnarchiveNotification: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    if (batch == null) {
        EmptyState("Batch not found", "This batch may have been cleared by retention or archive cleanup.")
        return
    }
    val filtered = batch.notifications.filter { it.matches(query) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            ExpressiveStatusCard(
                title = batch.title,
                body = "${batch.notificationCount} notifications · ${batch.releaseLabel}",
                icon = Icons.Filled.Inbox,
            )
        }
        item {
            SearchField(query, onQueryChange = { query = it }, placeholder = "Search this batch")
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            ) {
                OutlinedButton(
                    onClick = {
                        onArchiveBatch(batchId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Batch archived", "Undo")
                            if (result.name == "ActionPerformed") onUnarchiveBatch(batchId)
                        }
                    },
                ) {
                    Text("Archive batch")
                }
                Text(
                    "${filtered.size} notifications",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(filtered, key = { it.notificationKey }) { item ->
            NotificationRow(
                item = item,
                archiveLabel = if (item.isArchived) "Restore" else "Archive",
                onArchive = {
                    if (item.isArchived) onUnarchiveNotification(item.notificationKey) else onArchiveNotification(item.notificationKey)
                },
                onRestore = null,
                onMarkRead = { onMarkNotificationRead(item.notificationKey) },
            )
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationEntity,
    archiveLabel: String,
    onArchive: () -> Unit,
    onRestore: (() -> Unit)?,
    onMarkRead: () -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<NotificationEntity?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!item.isRead) onMarkRead()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onArchive()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !item.isRead,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeActionBackground(
                direction = dismissState.dismissDirection,
                archiveLabel = archiveLabel,
                markReadEnabled = !item.isRead,
            )
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selected = item }
                    .padding(MdSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(packageName = item.packageName, label = item.appLabel)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                        Text(item.appLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!item.isRead) {
                            Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
                        }
                    }
                    Text(
                        item.title ?: "Notification",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.text.isNullOrBlank()) {
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${formatTime(item.postedAtMillis)} · ${item.deliveryMode.name.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (selected != null) {
        NotificationDetailDialog(
            item = item,
            onDismiss = { selected = null },
            onOpenOriginal = {
                onMarkRead()
                openOriginalNotification(context, item)
                selected = null
            },
            onArchive = {
                onArchive()
                selected = null
            },
            onRestore = onRestore,
        )
    }
}

@Composable
private fun SwipeActionBackground(
    direction: SwipeToDismissBoxValue,
    archiveLabel: String,
    markReadEnabled: Boolean,
) {
    val isMarkRead = direction == SwipeToDismissBoxValue.StartToEnd
    val isArchive = direction == SwipeToDismissBoxValue.EndToStart
    val color = when {
        isMarkRead -> MaterialTheme.colorScheme.secondaryContainer
        isArchive -> MaterialTheme.colorScheme.tertiaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        isMarkRead -> MaterialTheme.colorScheme.onSecondaryContainer
        isArchive -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isMarkRead) Alignment.CenterStart else Alignment.CenterEnd
    val icon = if (isMarkRead) Icons.Filled.CheckCircle else if (archiveLabel == "Restore") Icons.Filled.Restore else Icons.Filled.Archive
    val label = if (isMarkRead) "Mark read" else archiveLabel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(color)
            .padding(horizontal = MdSpacing.sm),
        contentAlignment = alignment,
    ) {
        if (isArchive || markReadEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = contentColor)
                Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    item: NotificationEntity,
    onDismiss: () -> Unit,
    onOpenOriginal: () -> Unit,
    onArchive: () -> Unit,
    onRestore: (() -> Unit)?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title ?: item.appLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text(item.appLabel, style = MaterialTheme.typography.labelLarge)
                if (!item.text.isNullOrBlank()) Text(item.text)
                Text(formatDateTime(item.postedAtMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.channelId?.let { Text("Channel: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        confirmButton = {
            Button(onClick = onOpenOriginal) { Text("Open app/action") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onArchive) { Text(if (item.isArchived) "Restore" else "Archive") }
                onRestore?.let { TextButton(onClick = it) { Text("Restore") } }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

private fun openOriginalNotification(context: Context, item: NotificationEntity) {
    val opened = PendingIntentRegistry.send(item.notificationKey)
    if (!opened) {
        context.packageManager.getLaunchIntentForPackage(item.packageName)?.let(context::startActivity)
    }
}

@Composable
private fun RulesScreen(
    rules: List<AppRuleUi>,
    showSystemApps: Boolean,
    onShowSystemApps: (Boolean) -> Unit,
    onSetAppMode: (InstalledApp, DeliveryMode) -> Unit,
    onSetChannelMode: (ChannelRuleUi, DeliveryMode?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var modeFilter by remember { mutableStateOf<DeliveryMode?>(null) }
    var overridesOnly by remember { mutableStateOf(false) }
    val visibleRules = rules
        .filter { showSystemApps || !it.app.isSystemApp }
        .filter { it.matches(query) }
        .filter { modeFilter == null || it.app.mode == modeFilter || it.channels.any { channel -> channel.mode == modeFilter } }
        .filter { !overridesOnly || it.channels.any { channel -> channel.mode != null } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item { SearchField(query, onQueryChange = { query = it }, placeholder = "Search apps and channels") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                FilterChip(selected = modeFilter == null && !overridesOnly, onClick = { modeFilter = null; overridesOnly = false }, label = { Text("All") })
                FilterChip(selected = modeFilter == DeliveryMode.BATCH, onClick = { modeFilter = DeliveryMode.BATCH; overridesOnly = false }, label = { Text("Batched") })
                FilterChip(selected = modeFilter == DeliveryMode.INSTANT, onClick = { modeFilter = DeliveryMode.INSTANT; overridesOnly = false }, label = { Text("Instant") })
                FilterChip(selected = overridesOnly, onClick = { overridesOnly = !overridesOnly }, label = { Text("Custom") })
                FilterChip(selected = showSystemApps, onClick = { onShowSystemApps(!showSystemApps) }, label = { Text("System") })
            }
        }
        item {
            Text(
                "Sorted by recommendation and notification volume.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (visibleRules.isEmpty()) {
            item { EmptyState("No apps found", "Try a different search or enable system apps.") }
        }
        items(visibleRules, key = { it.app.packageName }) { appRule ->
            AppRuleCard(appRule, onSetAppMode, onSetChannelMode)
        }
    }
}

@Composable
private fun AppRuleCard(
    appRule: AppRuleUi,
    onSetAppMode: (InstalledApp, DeliveryMode) -> Unit,
    onSetChannelMode: (ChannelRuleUi, DeliveryMode?) -> Unit,
) {
    var expanded by remember(appRule.app.packageName) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                AppIcon(packageName = appRule.app.packageName, label = appRule.app.label, modifier = Modifier.size(40.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                ) {
                    Text(
                        appRule.app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        appRule.summaryLine(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                Box(Modifier.weight(1f, fill = true)) {
                    DeliveryModeSelector(appRule.app.mode) { onSetAppMode(appRule.app, it) }
                }
                if (appRule.channels.isNotEmpty()) {
                    IconButton(
                        onClick = { expanded = !expanded },
                    ) {
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Channels")
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    appRule.channels.forEach { channel ->
                        ChannelRuleRow(channel, appRule.app.mode, onSetChannelMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRuleRow(
    channel: ChannelRuleUi,
    appMode: DeliveryMode,
    onSetChannelMode: (ChannelRuleUi, DeliveryMode?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MdSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                channel.channelName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${channel.notificationCount} captured · ${channel.mode?.label() ?: "Same as app (${appMode.label()})"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChannelModeMenu(channel.mode) { onSetChannelMode(channel, it) }
    }
}

@Composable
private fun DeliveryModeSelector(value: DeliveryMode, onValue: (DeliveryMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
    ) {
        FilterChip(selected = value == DeliveryMode.BATCH, onClick = { onValue(DeliveryMode.BATCH) }, label = { Text("Batch") })
        FilterChip(selected = value == DeliveryMode.INSTANT, onClick = { onValue(DeliveryMode.INSTANT) }, label = { Text("Instant") })
    }
}

@Composable
private fun ChannelModeMenu(value: DeliveryMode?, onValue: (DeliveryMode?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(value?.label() ?: "Same as app")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Same as app") }, onClick = { onValue(null); expanded = false })
            DropdownMenuItem(text = { Text("Batched") }, onClick = { onValue(DeliveryMode.BATCH); expanded = false })
            DropdownMenuItem(text = { Text("Instant") }, onClick = { onValue(DeliveryMode.INSTANT); expanded = false })
        }
    }
}

@Composable
private fun DayTimeline(schedules: List<ScheduleRuleEntity>) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val disabled = MaterialTheme.colorScheme.onSurfaceVariant
    val needle = MaterialTheme.colorScheme.onSurface

    val cal = java.util.Calendar.getInstance()
    val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val trackH = 4.dp.toPx()
            val cy = size.height / 2f
            val dotR = 10.dp.toPx()

            drawRoundRect(
                color = track,
                topLeft = Offset(0f, cy - trackH / 2),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2),
            )

            fun minutesToX(minutes: Int) = size.width * ((minutes - 360 + 1440) % 1440 / 1440f)

            schedules.filter { it.isEnabled }.forEach { schedule ->
                drawCircle(color = primary, radius = dotR, center = Offset(minutesToX(schedule.releaseMinutes), cy))
            }
            schedules.filter { !it.isEnabled }.forEach { schedule ->
                drawCircle(color = disabled, radius = dotR, center = Offset(minutesToX(schedule.releaseMinutes), cy))
            }

            val nowX = minutesToX(nowMinutes)
            drawLine(
                color = needle.copy(alpha = 0.35f),
                start = Offset(nowX, cy - dotR - 4.dp.toPx()),
                end = Offset(nowX, cy + dotR + 4.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("6 AM", "12 PM", "6 PM", "12 AM", "6 AM").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScheduleScreen(
    schedules: List<ScheduleRuleEntity>,
    onUpdate: (ScheduleRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            DayTimeline(schedules)
        }
        if (schedules.isEmpty()) {
            item { EmptyState("No delivery times", "Tap + to add a time when notifications will be delivered.") }
        }
        items(schedules, key = { it.id }) { schedule ->
            BatchScheduleCard(schedule, onUpdate, onDelete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchScheduleCard(
    schedule: ScheduleRuleEntity,
    onUpdate: (ScheduleRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showTimePicker by remember(schedule.id) { mutableStateOf(false) }
    var expanded by remember(schedule.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = MdSpacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = MdSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        formatMinutes(schedule.releaseMinutes),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        activeDaysSummary(schedule.activeDaysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.padding(bottom = MdSpacing.sm), verticalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change time · ${formatMinutes(schedule.releaseMinutes)}")
                    }
                    WeekdaySelector(schedule.activeDaysMask) { mask ->
                        onUpdate(schedule.copy(activeDaysMask = mask))
                    }
                    AnimatedVisibility(visible = schedule.id > 0) {
                        TextButton(
                            onClick = { onDelete(schedule.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(MdSpacing.xs))
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = schedule.releaseMinutes / 60,
            initialMinute = schedule.releaseMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Delivery time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(onClick = {
                    onUpdate(schedule.copy(releaseMinutes = state.hour * 60 + state.minute))
                    showTimePicker = false
                }) { Text("Set time") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdaySelector(activeDaysMask: Int, onChanged: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
        Text("Active days", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            DayOfWeek.entries.forEach { day ->
                val bit = 1 shl (day.value - 1)
                val selected = activeDaysMask and bit != 0
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newMask = if (selected) activeDaysMask and bit.inv() else activeDaysMask or bit
                        onChanged(newMask)
                    },
                    label = { Text(day.name.take(1)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        Text(
            "On disabled days, notifications are delivered immediately instead of batched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsScreen(
    showSystemApps: Boolean,
    dynamicColor: Boolean,
    retentionDays: Int,
    onShowSystemApps: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onRetentionDays: (Int) -> Unit,
    onCleanupNow: () -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val canPost = rememberCanPostNotifications(context)
    val listenerEnabled = rememberNotificationListenerEnabled(context)
    val exactAlarmReady = rememberExactAlarmReady(context)
    val monetAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            PermissionCard(
                title = "Notification access",
                body = "Required to capture, hide, and batch notifications from other apps.",
                ready = listenerEnabled,
                action = "Open settings",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item {
            PermissionCard(
                title = "Post notifications",
                body = "Required so Notif Manager can notify you when held notifications are delivered.",
                ready = canPost,
                action = "Allow",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
        item {
            PermissionCard(
                title = "Exact timing",
                body = "Allows delivery close to your configured delivery time.",
                ready = exactAlarmReady,
                action = "Open settings",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                        )
                    }
                },
            )
        }
        item { SwitchRow("Show system apps", "Include system apps in Rules.", showSystemApps, onShowSystemApps) }
        item {
            SwitchRow(
                title = "Wallpaper colors",
                body = if (monetAvailable) {
                    "Match the app theme to your Android wallpaper colors."
                } else {
                    "Available on Android 12 and newer. This device uses the default purple theme."
                },
                checked = dynamicColor && monetAvailable,
                enabled = monetAvailable,
                onChecked = onDynamicColor,
            )
        }
        item {
            RetentionCard(retentionDays = retentionDays, onRetentionDays = onRetentionDays, onCleanupNow = onCleanupNow)
        }
        item {
            OutlinedButton(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("Replay onboarding")
            }
        }
    }
}

@Composable
private fun RetentionCard(retentionDays: Int, onRetentionDays: (Int) -> Unit, onCleanupNow: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(MdSpacing.sm), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            Text("Auto-clear history older than", style = MaterialTheme.typography.titleMedium)
            Text(retentionLabel(retentionDays), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                FilledTonalButton(onClick = { expanded = true }) { Text("Change retention") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf(7, 30, 90, AppSettings.RETENTION_NEVER).forEach { days ->
                        DropdownMenuItem(
                            text = { Text(retentionLabel(days)) },
                            onClick = {
                                onRetentionDays(days)
                                expanded = false
                            },
                        )
                    }
                }
            }
            OutlinedButton(onClick = onCleanupNow) { Text("Clear old records now") }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
        }
    }
}

@Composable
private fun OnboardingScreen(installedApps: List<InstalledApp>, onComplete: (List<InstalledApp>) -> Unit) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val selectedInstantPackages = remember { mutableStateListOf<String>() }
    LaunchedEffect(installedApps) {
        if (selectedInstantPackages.isEmpty()) {
            installedApps.filter { it.isRecommendedInstantApp }.forEach {
                selectedInstantPackages.add(it.packageName)
            }
        }
    }
    val nonSystemApps = installedApps
        .filter { !it.isSystemApp }
        .sortedWith(compareByDescending<InstalledApp> { it.isRecommendedInstantApp }.thenByDescending { it.isRecommendedHeavyApp }.thenBy { it.label })

    val pageCount = if (nonSystemApps.isNotEmpty()) 3 else 2
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pageCount - 1

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> OnboardingWelcomePage()
                1 -> OnboardingPermissionsPage(
                    context = context,
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                else -> OnboardingAppsPage(
                    nonSystemApps = nonSystemApps,
                    selectedInstantPackages = selectedInstantPackages,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = MdSpacing.xxs)
                            .size(if (selected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            ),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.weight(1f),
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        if (isLastPage) {
                            val instant = installedApps.filter { it.packageName in selectedInstantPackages }
                            onComplete(instant)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(if (isLastPage) "Get started" else "Next") }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp),
            ) {
                FlowerCanvas(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(MdSpacing.sm))
            Text(
                "Take back your attention",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Text(
                "Notif Manager holds distracting notifications and delivers them all at once when you're ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingPermissionsPage(context: Context, onRequestPostNotifications: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text("A few permissions", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "These let the app capture, hold, and re-deliver your notifications.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PermissionCard(
                title = "Notification access",
                body = "Required to capture and batch notifications from other apps.",
                ready = rememberNotificationListenerEnabled(context),
                action = "Open settings",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item {
            PermissionCard(
                title = "Post notifications",
                body = "Required to notify you when held notifications are delivered.",
                ready = rememberCanPostNotifications(context),
                action = "Allow",
                onClick = onRequestPostNotifications,
            )
        }
        item {
            PermissionCard(
                title = "Exact timing",
                body = "Improves accuracy for scheduled delivery times.",
                ready = rememberExactAlarmReady(context),
                action = "Open settings",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingAppsPage(nonSystemApps: List<InstalledApp>, selectedInstantPackages: MutableList<String>) {
    val instantCount = selectedInstantPackages.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
                modifier = Modifier.padding(bottom = MdSpacing.xs),
            ) {
                Text("What needs to ring through?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Selected apps bypass batching and notify you instantly. Everything else waits for your schedule.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (instantCount > 0) {
                    Text(
                        "$instantCount app${if (instantCount == 1) "" else "s"} set to instant",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        val recommended = nonSystemApps.filter { it.isRecommendedInstantApp }
        val rest = nonSystemApps.filter { !it.isRecommendedInstantApp }
        if (recommended.isNotEmpty()) {
            item {
                Text(
                    "Recommended",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MdSpacing.xs),
                )
            }
            items(recommended, key = { "rec_${it.packageName}" }) { app ->
                OnboardingAppRow(app, selectedInstantPackages)
            }
        }
        if (rest.isNotEmpty()) {
            item {
                Text(
                    "All apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MdSpacing.xs),
                )
            }
            items(rest, key = { "all_${it.packageName}" }) { app ->
                OnboardingAppRow(app, selectedInstantPackages)
            }
        }
    }
}

@Composable
private fun OnboardingAppRow(app: InstalledApp, selectedInstantPackages: MutableList<String>) {
    val selected = app.packageName in selectedInstantPackages
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                if (selected) selectedInstantPackages.remove(app.packageName)
                else selectedInstantPackages.add(app.packageName)
            },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(MdSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            AppIcon(packageName = app.packageName, label = app.label, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (selected) "Instant · rings through" else "Batched · waits for schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = selected, onCheckedChange = {
                if (selected) selectedInstantPackages.remove(app.packageName)
                else selectedInstantPackages.add(app.packageName)
            })
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    ready: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MdSpacing.xs))
                AssistChip(
                    onClick = {},
                    label = { Text(if (ready) "Ready" else "Needs setup") },
                    leadingIcon = if (ready) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }
            if (!ready) {
                Button(onClick = onClick) { Text(action) }
            }
        }
    }
}

@Composable
private fun InsightsScreen(insights: Insights) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            ExpressiveStatusCard(
                title = "Distraction shield",
                body = "${insights.distractionsSaved} interruptions kept out of your face.",
                icon = Icons.Filled.CheckCircle,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
                MetricCard("Received", insights.received, Icons.Filled.NotificationsActive, Modifier.weight(1f))
                MetricCard("Saved", insights.distractionsSaved, Icons.Filled.CheckCircle, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
                MetricCard("Batched", insights.batched, Icons.Filled.Inbox, Modifier.weight(1f))
                MetricCard("Instant", insights.instant, Icons.Filled.Schedule, Modifier.weight(1f))
            }
        }
        item {
            SectionCard("Busiest hours") {
                if (insights.busiestHours.isEmpty()) {
                    Text("No activity yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    insights.busiestHours.forEach { Text("${"%02d".format(it.hour)}:00 · ${it.count} notifications") }
                }
            }
        }
        item {
            SectionCard("Top apps") {
                if (insights.topApps.isEmpty()) {
                    Text("Per-app totals will appear after notifications are captured.")
                } else {
                    insights.topApps.take(8).forEach { app ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(app.appLabel, modifier = Modifier.weight(1f))
                            Text("${app.received} total · ${app.batched} saved")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(targetValue = value.toFloat(), label = "$label metric")
    Card(
        modifier = modifier.aspectRatio(1.05f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(animatedValue.roundToInt().toString(), style = MaterialTheme.typography.displaySmall)
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ExpressiveStatusCard(title: String, body: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(Modifier.fillMaxWidth()) {
            FlowerCanvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(150.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            )
            Row(
                modifier = Modifier.padding(MdSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun FlowerBadge(count: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        FlowerCanvas(Modifier.fillMaxSize(), MaterialTheme.colorScheme.secondaryContainer)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FlowerCanvas(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 4f
        repeat(8) { index ->
            val angle = (Math.PI * 2.0 * index / 8.0).toFloat()
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius,
                ),
            )
        }
        drawCircle(color = color, radius = radius * 1.15f, center = center)
    }
}

@Composable
private fun TopAppIcons(apps: List<String>, modifier: Modifier = Modifier) {
    if (apps.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Top apps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        apps.forEach { app ->
            AssistChip(onClick = {}, label = { Text(app, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        }
    }
}

@Composable
private fun SearchField(value: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = CircleShape,
    )
}

@Composable
private fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier.size(44.dp)) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    Surface(modifier = modifier.clip(MaterialTheme.shapes.medium), color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "$label icon", modifier = Modifier.fillMaxSize())
            } else {
                Text(label.take(1), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MdSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun rememberCanPostNotifications(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun rememberExactAlarmReady(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

@Composable
private fun rememberNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabled?.contains(context.packageName, ignoreCase = true) == true
}

private fun AppRuleUi.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val normalized = query.trim().lowercase()
    return app.label.contains(normalized, ignoreCase = true) ||
        app.packageName.contains(normalized, ignoreCase = true) ||
        channels.any {
            it.channelName.contains(normalized, ignoreCase = true) ||
                it.channelId.contains(normalized, ignoreCase = true)
        }
}

private fun AppRuleUi.summaryLine(): String {
    val parts = mutableListOf<String>()
    parts += when (app.notificationCount) {
        0 -> "No captured notifications yet"
        1 -> "1 notification captured"
        else -> "${app.notificationCount} notifications captured"
    }
    if (app.isRecommendedHeavyApp) {
        parts += "recommended noisy app"
    }
    if (channels.isNotEmpty()) {
        parts += "${channels.size} channels"
    }
    parts += app.mode.label()
    return parts.joinToString(" · ")
}

private fun NotificationEntity.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val normalized = query.trim().lowercase()
    return appLabel.contains(normalized, ignoreCase = true) ||
        packageName.contains(normalized, ignoreCase = true) ||
        title.orEmpty().contains(normalized, ignoreCase = true) ||
        text.orEmpty().contains(normalized, ignoreCase = true) ||
        channelId.orEmpty().contains(normalized, ignoreCase = true)
}

private fun DeliveryMode.label(): String {
    return when (this) {
        DeliveryMode.BATCH -> "Batched"
        DeliveryMode.INSTANT -> "Instant"
    }
}

private fun retentionLabel(days: Int): String {
    return when (days) {
        AppSettings.RETENTION_NEVER -> "Never auto-clear"
        1 -> "1 day"
        else -> "$days days"
    }
}

private fun activeDaysSummary(mask: Int): String {
    if (mask == ScheduleRuleEntity.ALL_DAYS_MASK) return "Every day"
    if (mask == 0) return "No active days"
    return DayOfWeek.entries
        .filter { mask and (1 shl (it.value - 1)) != 0 }
        .joinToString(" · ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
}

private fun formatCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0) return "delivering now"
    val totalMinutes = (remainingMillis / 60_000).toInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "in ${hours}h ${mins}m" else "in ${mins}m"
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

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "unknown"
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
}

private fun formatDateTime(millis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}
