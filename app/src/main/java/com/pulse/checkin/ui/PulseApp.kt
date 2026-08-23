package com.pulse.checkin.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.DayEventEditorSheet
import com.pulse.checkin.ui.components.HabitEditorSheet
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.screen.HistoryScreen
import com.pulse.checkin.ui.screen.DayEventsScreen
import com.pulse.checkin.ui.screen.SettingsScreen
import com.pulse.checkin.ui.screen.StatsScreen
import com.pulse.checkin.ui.screen.TodayScreen
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.i18n.rememberPulseStrings
import com.pulse.checkin.ui.theme.PulseTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PulseApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncUiState by viewModel.syncUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = rememberPulseStrings(uiState.preferences.appLanguage)
    var editorDraft by remember { mutableStateOf<HabitDraft?>(null) }
    val editorVisible = editorDraft != null
    var dayEventEditorDraft by remember { mutableStateOf<DayEventDraft?>(null) }
    val backgroundBlur by animateDpAsState(
        targetValue = if (editorVisible) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 260),
        label = "editor-background-blur",
    )
    val backgroundScrimAlpha by animateFloatAsState(
        targetValue = if (editorVisible) 0.16f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "editor-background-scrim",
    )
    var notificationsGranted by remember { mutableStateOf(checkNotificationsGranted(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = checkNotificationsGranted(context)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.exportBackup(uri)
                val message = result.fold(
                    onSuccess = { summary -> strings.exportSuccess(summary.habitCount, summary.eventCount) },
                    onFailure = { it.message ?: strings.exportFailed },
                )
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importBackup(uri)
                val message = result.fold(
                    onSuccess = { summary -> strings.importSuccess(summary.habitCount, summary.eventCount) },
                    onFailure = { it.message ?: strings.importFailed },
                )
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    CompositionLocalProvider(LocalPulseStrings provides strings) {
        PulseTheme(themeMode = uiState.preferences.themeMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            ) {
                Scaffold(
                    modifier = Modifier.blur(backgroundBlur),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        PulseBottomBar(
                            selectedTab = uiState.selectedTab,
                            onSelect = viewModel::selectTab,
                        )
                    },
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        Crossfade(targetState = uiState.selectedTab, label = "tab") { tab ->
                            when (tab) {
                                AppTab.TODAY -> TodayScreen(
                                    snapshot = uiState.todaySnapshot,
                                    onEditHabit = { habit -> editorDraft = HabitDraft.fromHabit(habit) },
                                    onAddHabit = { editorDraft = HabitDraft() },
                                    onCheckInHabit = viewModel::checkInHabit,
                                    onDeleteRecord = viewModel::deleteCheckInRecord,
                                    onDeleteHabit = viewModel::archiveHabit,
                                )
                                AppTab.HISTORY -> HistoryScreen(
                                    snapshot = uiState.historySnapshot,
                                    habits = uiState.habits,
                                    selectedHabitId = uiState.selectedHistoryHabitId,
                                    selectedDate = uiState.selectedDate,
                                    onPreviousMonth = { viewModel.shiftMonth(-1) },
                                    onNextMonth = { viewModel.shiftMonth(1) },
                                    onSelectDate = viewModel::selectDate,
                                    onSelectHabit = viewModel::selectHistoryHabit,
                                    onBackToCurrentMonth = { viewModel.selectDate(LocalDate.now()) },
                                    onBackfillHabit = viewModel::backfillHabit,
                                    onDeleteRecord = viewModel::deleteCheckInRecord,
                                )
                                AppTab.STATS -> StatsScreen(
                                    snapshot = uiState.yearSnapshot,
                                    onPreviousYear = { viewModel.shiftStatsYear(-1) },
                                    onNextYear = { viewModel.shiftStatsYear(1) },
                                    onBackToCurrentYear = viewModel::backToCurrentStatsYear,
                                    onSelectHabit = viewModel::selectStatsHabit,
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    themeMode = uiState.preferences.themeMode,
                                    appLanguage = uiState.preferences.appLanguage,
                                    notificationsGranted = notificationsGranted,
                                    syncState = syncUiState,
                                    onLogin = viewModel::login,
                                    onRegister = viewModel::register,
                                    onLogout = viewModel::logout,
                                    onSyncNow = viewModel::syncNow,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onAppLanguageChange = viewModel::setAppLanguage,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onExportData = {
                                        exportLauncher.launch("pulse-backup-${LocalDate.now()}.json")
                                    },
                                    onImportData = {
                                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                )
                                AppTab.DAY_EVENTS -> DayEventsScreen(
                                    dayEvents = uiState.dayEvents,
                                    onAdd = { dayEventEditorDraft = DayEventDraft() },
                                    onEdit = { dayEvent -> dayEventEditorDraft = DayEventDraft.fromDayEvent(dayEvent) },
                                    onDelete = viewModel::archiveDayEvent,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = backgroundScrimAlpha))
                                .zIndex(1f),
                        )
                    }
                }
                editorDraft?.let { draft ->
                    HabitEditorSheet(
                        initialDraft = draft,
                        notificationsGranted = notificationsGranted,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDismiss = { editorDraft = null },
                        onSave = {
                            viewModel.saveHabit(it)
                            editorDraft = null
                        },
                    )
                }
                dayEventEditorDraft?.let { draft ->
                    DayEventEditorSheet(
                        initialDraft = draft,
                        onDismiss = { dayEventEditorDraft = null },
                        onSave = {
                            viewModel.saveDayEvent(it)
                            dayEventEditorDraft = null
                        },
                    )
                }
            }
        }
    }

}

@Composable
private fun PulseBottomBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 12.dp else 16.dp
    val verticalPadding = if (compactLayout) 10.dp else 12.dp
    val itemSpacing = if (compactLayout) 6.dp else 8.dp

    GlassCard(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseBottomTab(
                tab = AppTab.TODAY,
                selectedTab = selectedTab,
                compactLayout = compactLayout,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PulseBottomTab(
                tab = AppTab.HISTORY,
                selectedTab = selectedTab,
                compactLayout = compactLayout,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PulseBottomTab(
                tab = AppTab.DAY_EVENTS,
                selectedTab = selectedTab,
                compactLayout = compactLayout,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PulseBottomTab(
                tab = AppTab.STATS,
                selectedTab = selectedTab,
                compactLayout = compactLayout,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PulseBottomTab(
                tab = AppTab.SETTINGS,
                selectedTab = selectedTab,
                compactLayout = compactLayout,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PulseBottomTab(
    tab: AppTab,
    selectedTab: AppTab,
    compactLayout: Boolean,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = tab == selectedTab
    val label = when (tab) {
        AppTab.TODAY -> LocalPulseStrings.current.tabToday
        AppTab.HISTORY -> LocalPulseStrings.current.tabHistory
        AppTab.STATS -> LocalPulseStrings.current.tabStats
        AppTab.SETTINGS -> LocalPulseStrings.current.tabSettings
        AppTab.DAY_EVENTS -> LocalPulseStrings.current.tabDayEvents
    }
    val icon = when (tab) {
        AppTab.TODAY -> PulseIconKind.TodayTab
        AppTab.HISTORY -> PulseIconKind.HistoryTab
        AppTab.STATS -> PulseIconKind.StatsTab
        AppTab.SETTINGS -> PulseIconKind.SettingsTab
        AppTab.DAY_EVENTS -> PulseIconKind.Calendar
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(vertical = if (compactLayout) 3.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PulseActionIcon(
                kind = icon,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                compactLayout = compactLayout,
                modifier = Modifier.size(if (compactLayout) 22.dp else 24.dp),
            )
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun checkNotificationsGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
