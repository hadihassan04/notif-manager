package com.tide.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tide.app.core.DeliveryTimeSuggester
import com.tide.app.core.InboxLayout
import com.tide.app.core.ManualOpen
import com.tide.app.core.OpenHoursCalculator
import com.tide.app.core.ScheduleCalculator
import com.tide.app.data.AppRuleUi
import com.tide.app.data.AppSettings
import com.tide.app.data.ChannelRuleUi
import com.tide.app.data.DeliveryMode
import com.tide.app.data.InboxBatch
import com.tide.app.data.InstalledApp
import com.tide.app.data.InstantWindowEntity
import com.tide.app.data.NotificationEntity
import com.tide.app.data.Repository
import com.tide.app.data.ScheduleRuleEntity
import com.tide.app.data.ThemeMode
import com.tide.app.notifications.PendingIntentRegistry
import com.tide.app.ui.AppIcon
import com.tide.app.ui.AppSelectionPane
import com.tide.app.ui.TideActionCard
import com.tide.app.ui.TideHeroCard
import com.tide.app.ui.TideMetricCard
import com.tide.app.ui.TideTopSlot
import com.tide.app.ui.TideWaves
import com.tide.app.ui.theme.MdSpacing
import com.tide.app.ui.theme.TideTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.Date

class MainActivity : ComponentActivity() {
    private var pendingBatchId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingBatchId = intent?.getStringExtra(EXTRA_BATCH_ID)
        enableEdgeToEdge()
        setContent {
            val app = application as TideApp
            val dynamicColor by app.settings.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
            val themeMode by app.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TideTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                val background = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    window.decorView.setBackgroundColor(background)
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.factory(app.repository, app.settings),
                )
                TideRoot(
                    viewModel = viewModel,
                    pendingBatchId = pendingBatchId,
                    onBatchIntentConsumed = { pendingBatchId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBatchId = intent.getStringExtra(EXTRA_BATCH_ID)
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
    val history: StateFlow<List<NotificationEntity>> = repository.recentNotifications.stateIn(
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
    val instantWindows: StateFlow<List<InstantWindowEntity>> = repository.instantWindows.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
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
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.SYSTEM,
    )
    val temporaryOpenUntilMillis: StateFlow<Long> = settings.temporaryOpenUntilMillis.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L,
    )
    val pauseBatching: StateFlow<Boolean> = settings.pauseBatching.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
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
            repository.initialize()
            repository.cleanupHistory(historyRetentionDays.value)
        }
    }

    fun setAppMode(app: InstalledApp, mode: DeliveryMode) {
        viewModelScope.launch { repository.setAppMode(app.packageName, app.label, mode) }
    }

    fun setAppModes(apps: List<InstalledApp>, mode: DeliveryMode) {
        viewModelScope.launch { repository.setAppModes(apps, mode) }
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

    fun addSchedule(
        releaseMinutes: Int,
        activeDaysMask: Int = ScheduleRuleEntity.ALL_DAYS_MASK,
    ) {
        viewModelScope.launch { repository.addSchedule(releaseMinutes, activeDaysMask) }
    }

    fun addInstantWindow() {
        viewModelScope.launch { repository.addInstantWindow() }
    }

    fun updateSchedule(schedule: ScheduleRuleEntity) {
        viewModelScope.launch { repository.updateSchedule(schedule) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }

    fun updateInstantWindow(window: InstantWindowEntity) {
        viewModelScope.launch { repository.updateInstantWindow(window) }
    }

    fun deleteInstantWindow(id: Long) {
        viewModelScope.launch { repository.deleteInstantWindow(id) }
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

    fun archiveNotifications(keys: List<String>) {
        viewModelScope.launch { repository.archiveNotifications(keys) }
    }

    fun unarchiveNotifications(keys: List<String>) {
        viewModelScope.launch { repository.unarchiveNotifications(keys) }
    }

    fun setShowSystemApps(enabled: Boolean) {
        viewModelScope.launch { settings.setShowSystemApps(enabled) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColorEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun startTemporaryOpen(untilMillis: Long) {
        viewModelScope.launch { repository.startTemporaryOpen(untilMillis) }
    }

    fun startIndefiniteOpen() {
        viewModelScope.launch { repository.startIndefiniteOpen() }
    }

    fun endTemporaryOpen() {
        viewModelScope.launch { repository.endTemporaryOpen() }
    }

    fun setHistoryRetentionDays(days: Int) {
        viewModelScope.launch {
            settings.setHistoryRetentionDays(days)
            repository.cleanupHistory(days)
        }
    }

    fun deliverNotificationsNow(keys: List<String>) {
        viewModelScope.launch { repository.deliverNotificationsNow(keys) }
    }

    fun archiveHistory(keys: List<String>) {
        archiveNotifications(keys)
    }

    fun cleanupHistoryNow() {
        viewModelScope.launch { repository.cleanupHistory(historyRetentionDays.value) }
    }

    fun completeOnboarding(instantApps: List<InstalledApp> = emptyList()) {
        viewModelScope.launch {
            repository.applyPrioritySelection(
                apps = installedApps.value,
                priorityPackages = instantApps.mapTo(mutableSetOf()) { it.packageName },
            )
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
    Inbox("inbox", "Inbox", Icons.Outlined.Inbox),
    Schedule("schedule", "Schedule", Icons.Outlined.Schedule),
    Priority("priority", "Apps", Icons.Outlined.Apps),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

private val PrimaryDestinations = listOf(Destination.Inbox, Destination.Schedule, Destination.Priority)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TideRoot(
    viewModel: MainViewModel,
    pendingBatchId: String?,
    onBatchIntentConsumed: () -> Unit,
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    if (!onboardingCompleted) {
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        val schedules by viewModel.schedules.collectAsStateWithLifecycle()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            OnboardingScreen(
                installedApps = installedApps,
                schedules = schedules,
                onAddSchedule = viewModel::addSchedule,
                onUpdateSchedule = viewModel::updateSchedule,
                onComplete = viewModel::completeOnboarding,
            )
        }
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val homePagerState = rememberPagerState(pageCount = { PrimaryDestinations.size })
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"
    val onSettings = currentRoute == Destination.Settings.route
    val currentPrimary = PrimaryDestinations[homePagerState.currentPage.coerceIn(0, PrimaryDestinations.lastIndex)]
    val title = if (onSettings) "Settings" else currentPrimary.label
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val openHours by viewModel.instantWindows.collectAsStateWithLifecycle()
    val temporaryOpenUntil by viewModel.temporaryOpenUntilMillis.collectAsStateWithLifecycle()
    val pauseBatching by viewModel.pauseBatching.collectAsStateWithLifecycle()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val manualOpen = ManualOpen(indefinite = pauseBatching, untilMillis = temporaryOpenUntil)
    val temporaryOpen = manualOpen.isActive(nowMillis)
    val scheduledOpen = remember(openHours, nowMillis) { OpenHoursCalculator().isOpenAt(nowMillis, openHours) }
    val isOpen = scheduledOpen || temporaryOpen
    var showTemporaryOpenDialog by remember { mutableStateOf(false) }
    var requestedBatchExpansion by remember { mutableStateOf<String?>(null) }

    fun goToPrimary(destination: Destination) {
        val index = PrimaryDestinations.indexOf(destination)
        if (index < 0) return
        if (onSettings) navController.popBackStack()
        scope.launch { homePagerState.animateScrollToPage(index) }
    }

    LaunchedEffect(pendingBatchId, onboardingCompleted) {
        if (pendingBatchId != null && onboardingCompleted) {
            requestedBatchExpansion = pendingBatchId
            goToPrimary(Destination.Inbox)
            onBatchIntentConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (onSettings) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!onSettings) {
                            if (scheduledOpen && !temporaryOpen) {
                                Text(
                                    "Open now",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = MdSpacing.xs),
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        if (temporaryOpen) viewModel.endTemporaryOpen()
                                        else showTemporaryOpenDialog = true
                                    },
                                ) {
                                    Text(if (temporaryOpen) "Start waiting" else "Allow all")
                                }
                            }
                        }
                        if (!onSettings) {
                            IconButton(
                                onClick = {
                                    navController.navigate(Destination.Settings.route) { launchSingleTop = true }
                                },
                            ) {
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
                if (!useRail && !onSettings) {
                    AppNavigationBar(selected = currentPrimary, onSelect = ::goToPrimary)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (useRail && !onSettings) {
                    AppNavigationRail(selected = currentPrimary, onSelect = ::goToPrimary)
                }
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.weight(1f),
                ) {
                    composable("home") {
                        val inbox by viewModel.inbox.collectAsStateWithLifecycle()
                        val notifications by viewModel.history.collectAsStateWithLifecycle()
                        val rules by viewModel.rulesUi.collectAsStateWithLifecycle()
                        val showSystemApps by viewModel.showSystemApps.collectAsStateWithLifecycle()
                        HorizontalPager(
                            state = homePagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            beyondViewportPageCount = 1,
                        ) { page ->
                            when (PrimaryDestinations[page]) {
                                Destination.Inbox -> NotificationsScreen(
                                    batches = inbox,
                                    notifications = notifications,
                                    schedules = schedules,
                                    isOpen = isOpen,
                                    manualOpen = manualOpen,
                                    nowMillis = nowMillis,
                                    snackbarHostState = snackbarHostState,
                                    requestedBatchExpansion = requestedBatchExpansion,
                                    onBatchExpansionConsumed = { requestedBatchExpansion = null },
                                    onArchiveNotifications = viewModel::archiveNotifications,
                                    onUnarchiveNotifications = viewModel::unarchiveNotifications,
                                    onDeliverNow = viewModel::deliverNotificationsNow,
                                )
                                Destination.Schedule -> ScheduleScreen(
                                    schedules = schedules,
                                    instantWindows = openHours,
                                    nowMillis = nowMillis,
                                    onAddDelivery = viewModel::addSchedule,
                                    onAddOpenHours = viewModel::addInstantWindow,
                                    onUpdate = viewModel::updateSchedule,
                                    onDelete = viewModel::deleteSchedule,
                                    onUpdateInstantWindow = viewModel::updateInstantWindow,
                                    onDeleteInstantWindow = viewModel::deleteInstantWindow,
                                )
                                Destination.Priority -> RulesScreen(
                                    rules = rules,
                                    showSystemApps = showSystemApps,
                                    onSetAppMode = viewModel::setAppMode,
                                    onSetChannelMode = viewModel::setChannelMode,
                                )
                                Destination.Settings -> Unit
                            }
                        }
                    }
                    composable(Destination.Settings.route) {
                        val showSystemApps by viewModel.showSystemApps.collectAsStateWithLifecycle()
                        val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
                        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                        val retentionDays by viewModel.historyRetentionDays.collectAsStateWithLifecycle()
                        SettingsScreen(
                            showSystemApps = showSystemApps,
                            dynamicColor = dynamicColor,
                            themeMode = themeMode,
                            retentionDays = retentionDays,
                            onShowSystemApps = viewModel::setShowSystemApps,
                            onDynamicColor = viewModel::setDynamicColorEnabled,
                            onThemeMode = viewModel::setThemeMode,
                            onRetentionDays = viewModel::setHistoryRetentionDays,
                            onCleanupNow = viewModel::cleanupHistoryNow,
                            onReplayOnboarding = viewModel::replayOnboarding,
                        )
                    }
                }
            }
        }
    }

    if (showTemporaryOpenDialog) {
        AlertDialog(
            onDismissRequest = { showTemporaryOpenDialog = false },
            title = { Text("Allow all notifications") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    Text("Waiting notifications will be delivered now. New notifications can interrupt you until this Open period ends.")
                    listOf(
                        Triple("30 minutes", "Open until ${formatTime(nowMillis + 30L * 60L * 1000L)}", nowMillis + 30L * 60L * 1000L),
                        Triple("1 hour", "Open until ${formatTime(nowMillis + 60L * 60L * 1000L)}", nowMillis + 60L * 60L * 1000L),
                        Triple(
                            "Until next delivery",
                            "Open until ${formatTime(
                                ScheduleCalculator().nextReleases(nowMillis, schedules)
                                    .minByOrNull { it.triggerAtMillis }
                                    ?.triggerAtMillis
                                    ?: nowMillis + 60L * 60L * 1000L,
                            )}",
                            ScheduleCalculator().nextReleases(nowMillis, schedules)
                                .minByOrNull { it.triggerAtMillis }
                                ?.triggerAtMillis
                                ?: nowMillis + 60L * 60L * 1000L,
                        ),
                    ).forEach { (label, body, until) ->
                        AddScheduleChoice(
                            title = label,
                            body = body,
                            icon = Icons.Filled.NotificationsActive,
                            onClick = {
                                viewModel.startTemporaryOpen(until)
                                showTemporaryOpenDialog = false
                            },
                        )
                    }
                    AddScheduleChoice(
                        title = "Until I turn it off",
                        body = "Stay Open until you tap Start waiting.",
                        icon = Icons.Filled.PlayCircle,
                        onClick = {
                            viewModel.startIndefiniteOpen()
                            showTemporaryOpenDialog = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemporaryOpenDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AddScheduleChoice(
    title: String,
    body: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppNavigationBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        PrimaryDestinations.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Spacer(Modifier.height(MdSpacing.sm))
        PrimaryDestinations.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

private data class NotificationGroup(
    val key: String,
    val items: List<NotificationEntity>,
) {
    val primary: NotificationEntity = items.maxBy { it.postedAtMillis }
    val count: Int = items.size
    val notificationKeys: List<String> = items.map { it.notificationKey }
    // A row's persistent UI state must follow the exact notifications it represents.
    // `key` describes a logical group (app/title/channel), so it remains the same when
    // a new notification joins that group. This identity is used for Compose item keys
    // to avoid transferring a dismissed swipe state to the updated row.
    val rowKey: String = notificationKeys.sorted().joinToString(separator = "\n")
    val title: String? = primary.title
    val text: String? = items
        .sortedByDescending { it.postedAtMillis }
        .mapNotNull { it.text?.takeIf(String::isNotBlank) }
        .take(3)
        .joinToString(" • ")
        .ifBlank { null }
}

private fun InboxBatch.notificationKeys(): List<String> {
    return notifications.map { it.notificationKey }
}

@Composable
private fun NotificationsScreen(
    batches: List<InboxBatch>,
    notifications: List<NotificationEntity>,
    schedules: List<ScheduleRuleEntity>,
    isOpen: Boolean,
    manualOpen: ManualOpen,
    nowMillis: Long,
    snackbarHostState: SnackbarHostState,
    requestedBatchExpansion: String?,
    onBatchExpansionConsumed: () -> Unit,
    onArchiveNotifications: (List<String>) -> Unit,
    onUnarchiveNotifications: (List<String>) -> Unit,
    onDeliverNow: (List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(requestedBatchExpansion) {
        if (requestedBatchExpansion != null) onBatchExpansionConsumed()
    }

    val sections = remember(batches, notifications, nowMillis) {
        InboxLayout.partition(batches, notifications, nowMillis)
    }
    val nextScheduleMillis = remember(schedules, nowMillis) {
        ScheduleCalculator().nextReleases(nowMillis, schedules).minByOrNull { it.triggerAtMillis }?.triggerAtMillis
    }
    val nextReleaseMillis = when {
        sections.dropUpcoming -> sections.drop?.releaseAtMillis
        else -> nextScheduleMillis
    }
    val remaining = (nextReleaseMillis ?: nowMillis) - nowMillis
    val manualActive = manualOpen.isActive(nowMillis)
    val heroValue = when {
        manualOpen.indefinite -> "On"
        isOpen && manualActive -> formatHeroCountdown(manualOpen.remainingMillis(nowMillis))
        isOpen -> "On"
        nextReleaseMillis != null -> formatHeroCountdown(remaining)
        else -> "—"
    }
    val heroCaption = when {
        manualOpen.indefinite -> "until you end it"
        isOpen && manualActive -> "until ${formatTime(manualOpen.untilMillis)}"
        isOpen -> "routine notifications can interrupt you"
        nextReleaseMillis != null -> "until ${formatTime(nextReleaseMillis)}"
        else -> "add a delivery time"
    }
    val tideFill = when {
        isOpen -> 0.82f
        nextReleaseMillis == null -> 0.4f
        remaining <= 0L -> 0.9f
        else -> {
            val sixthDay = 6f * 60f * 60f * 1000f
            (1f - (remaining / sixthDay).coerceIn(0f, 1f) * 0.55f)
        }
    }
    val dropGroups = remember(sections.drop) {
        digestNotifications(sections.drop?.notifications.orEmpty())
    }
    val heldGroups = remember(sections.held) { groupNotifications(sections.held) }
    val olderGroups = remember(sections.older) { groupNotifications(sections.older) }

    fun archiveWithUndo(keys: List<String>, message: String) {
        onArchiveNotifications(keys)
        scope.launch {
            val result = snackbarHostState.showSnackbar(message, "Undo")
            if (result.name == "ActionPerformed") onUnarchiveNotifications(keys)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(TideTopSlot.Padding)) {
            TideHeroCard(
                eyebrow = if (isOpen) "Open" else "Next drop",
                value = heroValue,
                caption = heroCaption,
                fill = tideFill,
                accent = isOpen,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            item(key = "section_drop") {
                InboxSectionLabel(
                    title = "Tide drop",
                    caption = sections.drop?.let { batch ->
                        val whenLabel = if (sections.dropUpcoming) {
                            "delivers at ${formatTime(batch.releaseAtMillis)}"
                        } else {
                            "delivered at ${formatTime(batch.releaseAtMillis)}"
                        }
                        "$whenLabel · ${batch.summaryText}"
                    } ?: "Nothing queued for the next delivery.",
                )
            }
            if (dropGroups.isEmpty()) {
                item(key = "drop_empty") {
                    EmptyState(
                        title = "Quiet drop",
                        body = if (isOpen) {
                            "Routine notifications are arriving now."
                        } else {
                            "The next delivery is empty so far."
                        },
                    )
                }
            } else {
                items(dropGroups, key = { "drop_${it.rowKey}" }) { group ->
                    NotificationRow(
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        ),
                        group = group,
                        archiveLabel = "Dismiss",
                        onArchive = { archiveWithUndo(group.notificationKeys, "Notification dismissed") },
                        onDeliverNow = if (sections.dropUpcoming) {
                            { onDeliverNow(group.notificationKeys) }
                        } else {
                            null
                        },
                    )
                }
            }

            item(key = "section_held") {
                InboxSectionLabel(
                    title = "Held",
                    caption = if (heldGroups.isEmpty()) {
                        "Nothing else waiting."
                    } else {
                        "${sections.held.size} waiting outside the drop"
                    },
                )
            }
            items(heldGroups, key = { "held_${it.rowKey}" }) { group ->
                NotificationRow(
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                    group = group,
                    archiveLabel = "Dismiss",
                    onArchive = { archiveWithUndo(group.notificationKeys, "Notification dismissed") },
                    onDeliverNow = { onDeliverNow(group.notificationKeys) },
                )
            }

            item(key = "section_older") {
                InboxSectionLabel(title = "Older", caption = if (olderGroups.isEmpty()) "Nothing delivered yet." else null)
            }
            items(olderGroups, key = { "older_${it.rowKey}" }) { group ->
                val archived = group.items.all { it.isArchived }
                NotificationRow(
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessLow),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                    group = group,
                    archiveLabel = if (archived) "Restore" else "Dismiss",
                    onArchive = {
                        if (archived) onUnarchiveNotifications(group.notificationKeys)
                        else archiveWithUndo(group.notificationKeys, "Notification dismissed")
                    },
                )
            }
        }
    }
}

@Composable
private fun InboxSectionLabel(title: String, caption: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!caption.isNullOrBlank()) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationRow(
    modifier: Modifier = Modifier,
    group: NotificationGroup,
    archiveLabel: String,
    onArchive: () -> Unit,
    onDeliverNow: (() -> Unit)? = null,
) {
    key(group.rowKey, archiveLabel, onDeliverNow != null) {
        NotificationRowContent(
            modifier = modifier,
            group = group,
            archiveLabel = archiveLabel,
            onArchive = onArchive,
            onDeliverNow = onDeliverNow,
        )
    }
}

@Composable
private fun NotificationRowContent(
    modifier: Modifier,
    group: NotificationGroup,
    archiveLabel: String,
    onArchive: () -> Unit,
    onDeliverNow: (() -> Unit)?,
) {
    val swipeEnabled = archiveLabel != "Restore"
    if (swipeEnabled) {
        SwipeableNotificationRow(
            modifier = modifier,
            onDismiss = onArchive,
            onDeliverNow = onDeliverNow,
        ) {
            NotificationCard(
                modifier = Modifier,
                group = group,
                archiveLabel = archiveLabel,
                swipeEnabled = true,
                onArchive = onArchive,
                onDeliverNow = onDeliverNow,
            )
        }
    } else {
        NotificationCard(
            modifier = modifier,
            group = group,
            archiveLabel = archiveLabel,
            swipeEnabled = false,
            onArchive = onArchive,
            onDeliverNow = null,
        )
    }
}

/** How far a row must travel, as a fraction of its width, before the release commits. */
private const val SwipeArchiveThreshold = 0.32f

/** A fling past this speed (px/s) commits even from a short drag. */
private const val SwipeArchiveVelocity = 1200f

/** Release below the threshold: a soft glide home that keeps a trace of the fling. */
private val SwipeReturnSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 200f)

/** Release past the threshold: the row keeps moving out rather than snapping away. */
private val SwipeExitSpec = spring<Float>(dampingRatio = 1f, stiffness = 160f)

/**
 * Android-shade swipe: left dismisses, right delivers now when that action exists.
 */
@Composable
private fun SwipeableNotificationRow(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDeliverNow: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    var dragTarget by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }
    var pastThreshold by remember { mutableStateOf(false) }

    val progress = if (width > 0f) (offsetX.value / width).coerceIn(-1f, 1f) else 0f
    val exitFade = if (settling) 1f - ((kotlin.math.abs(progress) - 0.7f) / 0.25f).coerceIn(0f, 1f) else 1f
    val minOffset = -width
    val maxOffset = if (onDeliverNow != null && width > 0f) width else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer { alpha = exitFade }
            .draggable(
                orientation = Orientation.Horizontal,
                enabled = !settling,
                state = rememberDraggableState { delta ->
                    val resistance = 1f - 0.45f * kotlin.math.abs(progress)
                    dragTarget = (dragTarget + delta * resistance).coerceIn(minOffset, maxOffset)
                    val crossed = width > 0f && kotlin.math.abs(dragTarget / width) >= SwipeArchiveThreshold
                    if (crossed != pastThreshold) {
                        pastThreshold = crossed
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    scope.launch { offsetX.snapTo(dragTarget) }
                },
                onDragStarted = { dragTarget = offsetX.value },
                onDragStopped = { velocity ->
                    val fraction = if (width > 0f) dragTarget / width else 0f
                    val dismissFling = velocity < -SwipeArchiveVelocity && fraction < -0.1f
                    val deliverFling = onDeliverNow != null && velocity > SwipeArchiveVelocity && fraction > 0.1f
                    val dismiss = fraction <= -SwipeArchiveThreshold || dismissFling
                    val deliver = onDeliverNow != null && (fraction >= SwipeArchiveThreshold || deliverFling)
                    if (width > 0f && (dismiss || deliver)) {
                        settling = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val target = if (dismiss) -width else width
                        val action = if (dismiss) onDismiss else onDeliverNow
                        var handedOff = false
                        offsetX.animateTo(target, SwipeExitSpec, initialVelocity = velocity) {
                            if (!handedOff && kotlin.math.abs(value) >= width * 0.95f) {
                                handedOff = true
                                action?.invoke()
                            }
                        }
                        if (!handedOff) action?.invoke()
                    } else {
                        pastThreshold = false
                        offsetX.animateTo(0f, SwipeReturnSpec, initialVelocity = velocity)
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    when {
                        offsetX.value > 0f -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                )
                .padding(horizontal = MdSpacing.sm),
            contentAlignment = if (offsetX.value > 0f) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            val reveal = (kotlin.math.abs(progress) / SwipeArchiveThreshold).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
                modifier = Modifier.graphicsLayer {
                    alpha = reveal
                    scaleX = 0.8f + 0.2f * reveal
                    scaleY = 0.8f + 0.2f * reveal
                },
            ) {
                if (offsetX.value > 0f) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Deliver now",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        "Dismiss",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Icon(
                        Icons.Filled.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
            content()
        }
    }
}

@Composable
private fun NotificationCard(
    modifier: Modifier,
    group: NotificationGroup,
    archiveLabel: String,
    swipeEnabled: Boolean,
    onArchive: () -> Unit,
    onDeliverNow: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val item = group.primary
    val actions = buildList {
        add(
            CustomAccessibilityAction(archiveLabel) {
                onArchive()
                true
            },
        )
        if (onDeliverNow != null) {
            add(
                CustomAccessibilityAction("Deliver now") {
                    onDeliverNow()
                    true
                },
            )
        }
        add(
            CustomAccessibilityAction("Open") {
                openOriginalNotification(context, item)
                true
            },
        )
    }
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { customActions = actions },
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { openOriginalNotification(context, item) }
                    .padding(MdSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                AppIcon(packageName = item.packageName, label = item.appLabel, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                        Text(item.appLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        group.title?.let { if (group.count > 1) "$it (${group.count})" else it } ?: "Notification",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!group.text.isNullOrBlank()) {
                        Text(
                            group.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${formatTime(item.postedAtMillis)} · ${item.deliveryMode.label()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!swipeEnabled) {
                    IconButton(onClick = onArchive) {
                        Icon(
                            Icons.Filled.Restore,
                            contentDescription = archiveLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun openOriginalNotification(context: Context, item: NotificationEntity) {
    val opened = PendingIntentRegistry.send(item.notificationKey)
    if (!opened) {
        context.packageManager.getLaunchIntentForPackage(item.packageName)?.let(context::startActivity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RulesScreen(
    rules: List<AppRuleUi>,
    showSystemApps: Boolean,
    onSetAppMode: (InstalledApp, DeliveryMode) -> Unit,
    onSetChannelMode: (ChannelRuleUi, DeliveryMode?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var openedPackage by remember { mutableStateOf<String?>(null) }
    val visibleRules = remember(rules, showSystemApps, query) {
        rules
            .filter { showSystemApps || !it.app.isSystemApp }
            .filter { it.matches(query) }
    }
    val apps = visibleRules.map { it.app }
    val instantCount = apps.count { it.mode == DeliveryMode.INSTANT }

    AppSelectionPane(
        apps = apps,
        isInstant = { it.mode == DeliveryMode.INSTANT },
        onToggle = { app, instant ->
            onSetAppMode(app, if (instant) DeliveryMode.INSTANT else DeliveryMode.BATCH)
        },
        query = query,
        onQueryChange = { query = it },
        header = {
            Text(
                "Priority apps always reach you, even while routine notifications are waiting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (instantCount > 0) {
                Text(
                    "$instantCount instant app${if (instantCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onLongClick = { openedPackage = it.packageName },
        exceptionCount = { app ->
            rules.firstOrNull { it.app.packageName == app.packageName }
                ?.channels
                ?.count { it.mode != null && it.mode != app.mode }
                ?: 0
        },
        searchPlaceholder = "Search apps",
    )

    val opened = openedPackage?.let { packageName -> rules.firstOrNull { it.app.packageName == packageName } }
    if (opened != null) {
        AppRuleSheet(
            appRule = opened,
            onSetAppMode = onSetAppMode,
            onSetChannelMode = onSetChannelMode,
            onDismiss = { openedPackage = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRuleSheet(
    appRule: AppRuleUi,
    onSetAppMode: (InstalledApp, DeliveryMode) -> Unit,
    onSetChannelMode: (ChannelRuleUi, DeliveryMode?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.sm)
                .padding(bottom = MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                AppIcon(packageName = appRule.app.packageName, label = appRule.app.label, modifier = Modifier.size(48.dp))
                Column(Modifier.weight(1f)) {
                    Text(appRule.app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        appRule.summaryLine(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DeliveryModeSelector(appRule.app.mode) { onSetAppMode(appRule.app, it) }
            if (appRule.channels.isNotEmpty()) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                appRule.channels.forEach { channel ->
                    ChannelRuleRow(channel, appRule.app.mode, onSetChannelMode)
                }
            }
        }
    }
}

private enum class InstantWindowTimeTarget {
    Start,
    End,
}

@Composable
private fun TimeChip(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMinutes(minutes), style = MaterialTheme.typography.titleMedium)
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
                "${channel.notificationCount} captured · ${channel.mode?.label() ?: "Uses app setting (${appMode.label()})"}",
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
        FilterChip(
            selected = value == DeliveryMode.BATCH,
            onClick = { onValue(DeliveryMode.BATCH) },
            label = { Text("Batch") },
            leadingIcon = { Icon(DeliveryMode.BATCH.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = value == DeliveryMode.INSTANT,
            onClick = { onValue(DeliveryMode.INSTANT) },
            label = { Text("Instant") },
            leadingIcon = { Icon(DeliveryMode.INSTANT.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun ChannelModeMenu(value: DeliveryMode?, onValue: (DeliveryMode?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            value?.let {
                Icon(it.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MdSpacing.xxs))
            }
            Text(value?.label() ?: "Uses app setting")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Uses app setting") }, onClick = { onValue(null); expanded = false })
            DeliveryMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null) },
                    onClick = { onValue(mode); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleScreen(
    schedules: List<ScheduleRuleEntity>,
    instantWindows: List<InstantWindowEntity>,
    nowMillis: Long,
    onAddDelivery: (Int, Int) -> Unit,
    onAddOpenHours: () -> Unit,
    onUpdate: (ScheduleRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdateInstantWindow: (InstantWindowEntity) -> Unit,
    onDeleteInstantWindow: (Long) -> Unit,
) {
    val nextDelivery = remember(schedules, nowMillis) {
        ScheduleCalculator().nextReleases(nowMillis, schedules).minByOrNull { it.triggerAtMillis }
    }
    val nextSchedule = nextDelivery?.schedule
    val remaining = (nextDelivery?.triggerAtMillis ?: nowMillis) - nowMillis
    val tideFill = when {
        nextDelivery == null -> 0.4f
        remaining <= 0L -> 0.9f
        else -> {
            val sixthDay = 6f * 60f * 60f * 1000f
            (1f - (remaining / sixthDay).coerceIn(0f, 1f) * 0.55f)
        }
    }
    var editingSchedule by remember { mutableStateOf<ScheduleRuleEntity?>(null) }
    var editingWindow by remember { mutableStateOf<InstantWindowEntity?>(null) }
    val openWindow = instantWindows.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(TideTopSlot.Padding)) {
            TideHeroCard(
                eyebrow = "Next delivery",
                value = nextSchedule?.let { formatMinutes(it.releaseMinutes) } ?: "—",
                caption = when {
                    nextSchedule != null -> activeDaysSummary(nextSchedule.activeDaysMask)
                    else -> "Add a delivery time so waiting notifications can land"
                },
                fill = tideFill,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            items(schedules, key = { "schedule_${it.id}" }) { schedule ->
            TideMetricCard(
                label = activeDaysSummary(schedule.activeDaysMask),
                value = formatMinutes(schedule.releaseMinutes),
                accent = nextSchedule?.id == schedule.id,
                onClick = { editingSchedule = schedule },
                onLongClick = { if (schedule.id > 0) onDelete(schedule.id) },
            )
        }
        item {
            TideMetricCard(
                label = "Add a delivery",
                value = "+",
                onClick = {
                    val suggested = DeliveryTimeSuggester.suggest(
                        existingMinutes = schedules.filter { it.isEnabled }.map { it.releaseMinutes },
                        nowMinutes = minuteOfDay(nowMillis),
                    )
                    editingSchedule = ScheduleRuleEntity(
                        releaseMinutes = suggested,
                        updatedAtMillis = 0,
                    )
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (openWindow == null) {
                TideActionCard(
                    title = "Open hours",
                    body = "None yet · add a range that releases waiting notifications",
                    onClick = onAddOpenHours,
                )
            } else {
                TideActionCard(
                    title = "${formatMinutes(openWindow.startMinutes)}–${formatMinutes(openWindow.endMinutes)}",
                    body = if (instantWindows.size > 1) {
                        "Open hours · ${activeDaysSummary(openWindow.activeDaysMask)} · ${instantWindows.size} ranges"
                    } else {
                        "Open hours · ${activeDaysSummary(openWindow.activeDaysMask)}"
                    },
                    onClick = { editingWindow = openWindow },
                )
            }
        }
        if (instantWindows.size > 1) {
            items(
                instantWindows.drop(1),
                key = { "open_${it.id}" },
                span = { GridItemSpan(maxLineSpan) },
            ) { window ->
                TideActionCard(
                    title = "${formatMinutes(window.startMinutes)}–${formatMinutes(window.endMinutes)}",
                    body = "Open hours · ${activeDaysSummary(window.activeDaysMask)}",
                    onClick = { editingWindow = window },
                )
            }
        }
        }
    }

    editingSchedule?.let { schedule ->
        DeliveryTimeEditDialog(
            schedule = schedule,
            onUpdate = { updated ->
                editingSchedule = updated
                if (updated.id > 0) onUpdate(updated)
            },
            onConfirm = { confirmed ->
                if (confirmed.id > 0) {
                    onUpdate(confirmed)
                } else {
                    onAddDelivery(confirmed.releaseMinutes, confirmed.activeDaysMask)
                }
                editingSchedule = null
            },
            onDelete = {
                onDelete(it)
                editingSchedule = null
            },
            onDismiss = { editingSchedule = null },
        )
    }
    editingWindow?.let { window ->
        OpenHoursEditDialog(
            window = window,
            onUpdate = {
                onUpdateInstantWindow(it)
                editingWindow = it
            },
            onDelete = {
                onDeleteInstantWindow(it)
                editingWindow = null
            },
            onDismiss = { editingWindow = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryTimeEditDialog(
    schedule: ScheduleRuleEntity,
    onUpdate: (ScheduleRuleEntity) -> Unit,
    onConfirm: (ScheduleRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = schedule.releaseMinutes / 60,
        initialMinute = schedule.releaseMinutes % 60,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery time") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                TimePicker(state = state)
                WeekdaySelector(
                    activeDaysMask = schedule.activeDaysMask,
                    onChanged = { onUpdate(schedule.copy(activeDaysMask = it)) },
                )
                if (schedule.id > 0) {
                    TextButton(
                        onClick = { onDelete(schedule.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(MdSpacing.xs))
                        Text("Remove delivery time")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(schedule.copy(releaseMinutes = state.hour * 60 + state.minute))
                },
            ) { Text("Set time") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenHoursEditDialog(
    window: InstantWindowEntity,
    onUpdate: (InstantWindowEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<InstantWindowTimeTarget?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open hours") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = window.isEnabled,
                        onCheckedChange = { onUpdate(window.copy(isEnabled = it)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    TimeChip(
                        label = "Start",
                        minutes = window.startMinutes,
                        onClick = { editing = InstantWindowTimeTarget.Start },
                        modifier = Modifier.weight(1f),
                    )
                    TimeChip(
                        label = "End",
                        minutes = window.endMinutes,
                        onClick = { editing = InstantWindowTimeTarget.End },
                        modifier = Modifier.weight(1f),
                    )
                }
                WeekdaySelector(
                    activeDaysMask = window.activeDaysMask,
                    onChanged = { onUpdate(window.copy(activeDaysMask = it)) },
                    title = "Open days",
                    body = "The queue is released when Open hours begin on these days.",
                )
                if (window.id > 0) {
                    TextButton(
                        onClick = { onDelete(window.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(MdSpacing.xs))
                        Text("Remove Open hours")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    editing?.let { target ->
        val initialMinutes = if (target == InstantWindowTimeTarget.Start) window.startMinutes else window.endMinutes
        val state = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (target == InstantWindowTimeTarget.Start) "Open hours start" else "Open hours end") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = state.hour * 60 + state.minute
                        if (target == InstantWindowTimeTarget.Start) {
                            onUpdate(window.copy(startMinutes = minutes))
                        } else {
                            onUpdate(window.copy(endMinutes = minutes))
                        }
                        editing = null
                    },
                ) { Text("Set time") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchScheduleCard(
    schedule: ScheduleRuleEntity,
    onUpdate: (ScheduleRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
    allowDelete: Boolean = true,
) {
    var showTimePicker by remember(schedule.id) { mutableStateOf(false) }
    var expanded by remember(schedule.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = MdSpacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MdSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showTimePicker = true }
                        .padding(vertical = MdSpacing.xxs),
                ) {
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
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onUpdate(schedule.copy(isEnabled = it)) },
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse schedule editor" else "Expand schedule editor",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.padding(bottom = MdSpacing.sm), verticalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
                    WeekdaySelector(
                        activeDaysMask = schedule.activeDaysMask,
                        onChanged = { mask -> onUpdate(schedule.copy(activeDaysMask = mask)) },
                    )
                    AnimatedVisibility(visible = allowDelete && schedule.id > 0) {
                        TextButton(
                            onClick = { onDelete(schedule.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(MdSpacing.xs))
                            Text("Remove delivery time")
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
private fun WeekdaySelector(
    activeDaysMask: Int,
    onChanged: (Int) -> Unit,
    title: String = "Delivery days",
    body: String = "Waiting notifications are delivered on the selected days.",
) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Seven circles, one letter each: the whole week fits without scrolling, and
        // the pattern of a schedule can be read at a glance.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
        ) {
            DayOfWeek.entries.forEach { day ->
                val bit = 1 shl (day.value - 1)
                val selected = activeDaysMask and bit != 0
                val label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .clickable {
                            val newMask = if (selected) activeDaysMask and bit.inv() else activeDaysMask or bit
                            onChanged(newMask)
                        }
                        .semantics {
                            this.selected = selected
                            contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    showSystemApps: Boolean,
    dynamicColor: Boolean,
    themeMode: ThemeMode,
    retentionDays: Int,
    onShowSystemApps: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onRetentionDays: (Int) -> Unit,
    onCleanupNow: () -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    var permissionRefresh by remember { mutableLongStateOf(0L) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefresh++
    }
    val permissions = rememberPermissionStatus(context, permissionRefresh)
    val monetAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item { SettingsSectionLabel("Setup") }
        item {
            PermissionCard(
                title = "Notification access",
                body = "So Tide can hold notifications and deliver them later.",
                ready = permissions.listenerEnabled,
                action = "Allow",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item {
            PermissionCard(
                title = "Delivery notifications",
                body = "So Tide can tell you when a delivery arrives.",
                ready = permissions.canPost,
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
                title = "Precise delivery",
                body = "So deliveries land at the minute you chose.",
                ready = permissions.exactAlarmReady,
                action = "Allow",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                        )
                    }
                },
            )
        }
        item { SettingsSectionLabel("Appearance") }
        item { ThemeModeCard(themeMode, onThemeMode) }
        item {
            SwitchRow(
                title = "Wallpaper colors",
                body = if (monetAvailable) {
                    "Match the app theme to your Android wallpaper colors."
                } else {
                    "Available on Android 12 and newer. This device uses the Tide palette."
                },
                checked = dynamicColor && monetAvailable,
                enabled = monetAvailable,
                onChecked = onDynamicColor,
            )
        }
        item { SettingsSectionLabel("Notification management") }
        item {
            SwitchRow(
                title = "Show system apps",
                body = "System apps stay Instant unless you change them. This only lists them with the rest.",
                checked = showSystemApps,
                onChecked = onShowSystemApps,
            )
        }
        item {
            RetentionCard(retentionDays = retentionDays, onRetentionDays = onRetentionDays, onCleanupNow = onCleanupNow)
        }
        item { SettingsSectionLabel("Help") }
        item {
            OutlinedButton(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("Review setup")
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = MdSpacing.xs),
    )
}

@Composable
private fun ThemeModeCard(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose an appearance or follow your Android setting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RetentionCard(retentionDays: Int, onRetentionDays: (Int) -> Unit, onCleanupNow: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
    ) {
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
private fun OnboardingScreen(
    installedApps: List<InstalledApp>,
    schedules: List<ScheduleRuleEntity>,
    onAddSchedule: (Int, Int) -> Unit,
    onUpdateSchedule: (ScheduleRuleEntity) -> Unit,
    onComplete: (List<InstalledApp>) -> Unit,
) {
    val context = LocalContext.current
    var permissionRefresh by remember { mutableLongStateOf(0L) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefresh++
    }
    val permissions = rememberPermissionStatus(context, permissionRefresh)
    val selectedInstantPackages = remember { mutableStateListOf<String>() }
    var seededDefaults by remember { mutableStateOf(false) }
    val nonSystemApps = remember(installedApps) { installedApps.filter { !it.isSystemApp } }
    LaunchedEffect(nonSystemApps) {
        if (nonSystemApps.isEmpty()) return@LaunchedEffect
        if (!seededDefaults) {
            nonSystemApps.filter { it.role.defaultsToInstant }.forEach { app ->
                if (app.packageName !in selectedInstantPackages) selectedInstantPackages.add(app.packageName)
            }
            seededDefaults = true
        }
    }

    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pageCount - 1
    val requiredPermissionsReady = permissions.listenerEnabled && permissions.canPost
    val hasDeliveryTime = schedules.any { it.isEnabled && it.activeDaysMask != 0 }
    val canContinue = when (pagerState.currentPage) {
        1 -> requiredPermissionsReady
        3 -> hasDeliveryTime
        else -> true
    }

    // Onboarding draws edge to edge with no scaffold of its own, so it has to keep
    // itself clear of the notch and the gesture bar.
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> OnboardingWelcomePage()
                1 -> OnboardingPermissionsPage(
                    context = context,
                    permissions = permissions,
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                2 -> OnboardingAppsPage(
                    nonSystemApps = nonSystemApps,
                    selectedInstantPackages = selectedInstantPackages,
                )
                else -> OnboardingSchedulePage(schedules, onAddSchedule, onUpdateSchedule)
            }
        }

        Box {
            TideWaves(
                modifier = Modifier
                    .matchParentSize(),
                color = MaterialTheme.colorScheme.primary,
            )
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
            if (!canContinue) {
                Text(
                    if (pagerState.currentPage == 1) {
                        "Allow the permissions above to continue."
                    } else {
                        "Keep at least one delivery time enabled so waiting notifications have a safe release time."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    enabled = canContinue,
                ) {
                    Text(if (isLastPage) "Finish setup" else "Continue")
                    Spacer(Modifier.size(MdSpacing.xs))
                    Icon(
                        if (isLastPage) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun OnboardingSchedulePage(
    schedules: List<ScheduleRuleEntity>,
    onAddSchedule: (Int, Int) -> Unit,
    onUpdateSchedule: (ScheduleRuleEntity) -> Unit,
) {
    var draft by remember { mutableStateOf<ScheduleRuleEntity?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text("When should waiting notifications arrive?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Review the suggested delivery times. You can change them now or refine the schedule later.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (schedules.isEmpty()) {
            item {
                Button(
                    onClick = {
                        draft = ScheduleRuleEntity(
                            releaseMinutes = DeliveryTimeSuggester.suggest(
                                existingMinutes = emptyList(),
                                nowMinutes = minuteOfDay(System.currentTimeMillis()),
                            ),
                            updatedAtMillis = 0,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add a delivery time")
                }
            }
        }
        items(schedules, key = { "onboarding_schedule_${it.id}" }) { schedule ->
            BatchScheduleCard(
                schedule = schedule,
                onUpdate = onUpdateSchedule,
                onDelete = {},
                allowDelete = false,
            )
        }
    }
    draft?.let { schedule ->
        DeliveryTimeEditDialog(
            schedule = schedule,
            onUpdate = { draft = it },
            onConfirm = { confirmed ->
                onAddSchedule(confirmed.releaseMinutes, confirmed.activeDaysMask)
                draft = null
            },
            onDelete = { draft = null },
            onDismiss = { draft = null },
        )
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
            Surface(
                modifier = Modifier.size(104.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                TideWaves(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(MdSpacing.sm))
            Text(
                "Take back your attention",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Text(
                "Batched notifications wait for times you choose. Instant apps still reach you right away.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingPermissionsPage(
    context: Context,
    permissions: PermissionStatus,
    onRequestPostNotifications: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text("Required setup", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Three permissions, then you are set.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PermissionCard(
                title = "Notification access",
                body = "So Tide can hold notifications and deliver them later.",
                ready = permissions.listenerEnabled,
                action = "Allow",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item {
            PermissionCard(
                title = "Delivery notifications",
                body = "So Tide can tell you when a delivery arrives.",
                ready = permissions.canPost,
                action = "Allow",
                onClick = onRequestPostNotifications,
            )
        }
        item {
            PermissionCard(
                title = "Precise delivery",
                body = "So deliveries land at the minute you chose.",
                ready = permissions.exactAlarmReady,
                action = "Allow",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingAppsPage(nonSystemApps: List<InstalledApp>, selectedInstantPackages: MutableList<String>) {
    var query by remember { mutableStateOf("") }
    val instantCount = selectedInstantPackages.size
    AppSelectionPane(
        apps = nonSystemApps,
        isInstant = { it.packageName in selectedInstantPackages },
        onToggle = { app, instant ->
            if (instant) {
                if (app.packageName !in selectedInstantPackages) selectedInstantPackages.add(app.packageName)
            } else {
                selectedInstantPackages.remove(app.packageName)
            }
        },
        query = query,
        onQueryChange = { query = it },
        header = {
            Text("What should always reach you?", style = MaterialTheme.typography.headlineSmall)
            if (instantCount > 0) {
                Text(
                    "$instantCount instant app${if (instantCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
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
            }
            // One control, and it only ever has two things to say: ask, or confirm.
            if (ready) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Allowed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else {
                Button(onClick = onClick) { Text(action) }
            }
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

private data class PermissionStatus(
    val canPost: Boolean,
    val exactAlarmReady: Boolean,
    val listenerEnabled: Boolean,
)

@Composable
private fun rememberPermissionStatus(context: Context, refreshToken: Long = 0L): PermissionStatus {
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) { mutableStateOf(readPermissionStatus(context)) }
    fun refresh() {
        status = readPermissionStatus(context)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refreshToken) { refresh() }
    return status
}

private fun readPermissionStatus(context: Context): PermissionStatus {
    return PermissionStatus(
        canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        exactAlarmReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        listenerEnabled = notificationListenerEnabled(context),
    )
}

private fun notificationListenerEnabled(context: Context): Boolean {
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

private fun NotificationGroup.matches(query: String): Boolean {
    if (query.isBlank()) return true
    return items.any { it.matches(query) }
}

private fun digestNotifications(items: List<NotificationEntity>): List<NotificationGroup> {
    return items
        .groupBy { it.packageName }
        .values
        .map { groupedItems ->
            NotificationGroup(
                key = "digest_${groupedItems.first().packageName}",
                items = groupedItems.sortedByDescending { it.postedAtMillis },
            )
        }
        .sortedByDescending { it.primary.postedAtMillis }
}

private fun groupNotifications(items: List<NotificationEntity>): List<NotificationGroup> {
    return items
        .groupBy { it.groupKey() }
        .values
        .map { groupedItems ->
            NotificationGroup(
                key = groupedItems.first().groupKey(),
                items = groupedItems.sortedByDescending { it.postedAtMillis },
            )
        }
        .sortedByDescending { it.primary.postedAtMillis }
}

private fun NotificationEntity.groupKey(): String {
    return listOf(
        batchId.orEmpty(),
        packageName,
        channelId.orEmpty(),
        title?.trim()?.lowercase().orEmpty(),
        deliveryMode.name,
        isArchived.toString(),
    ).joinToString("\n")
}

private fun DeliveryMode.label(): String {
    return when (this) {
        DeliveryMode.BATCH -> "Batch"
        DeliveryMode.INSTANT -> "Instant"
    }
}

/** Batch pauses a notification; instant lets it ring straight through. */
private fun DeliveryMode.icon(): ImageVector = when (this) {
    DeliveryMode.BATCH -> Icons.Filled.PauseCircle
    DeliveryMode.INSTANT -> Icons.Filled.NotificationsActive
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

private fun formatHeroCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0) return "now"
    val totalMinutes = (remainingMillis / 60_000).toInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun minuteOfDay(nowMillis: Long): Int {
    val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(java.time.ZoneId.systemDefault())
    return now.hour * 60 + now.minute
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
