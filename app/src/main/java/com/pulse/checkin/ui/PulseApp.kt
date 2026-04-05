package com.pulse.checkin.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HabitEditorSheet
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.screen.HistoryScreen
import com.pulse.checkin.ui.screen.SettingsScreen
import com.pulse.checkin.ui.screen.StatsScreen
import com.pulse.checkin.ui.screen.TodayScreen
import com.pulse.checkin.ui.theme.PulseTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PulseApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editorDraft by remember { mutableStateOf<HabitDraft?>(null) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsGranted(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = checkNotificationsGranted(context)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.exportBackup(uri)
                Toast.makeText(context, result.getOrElse { it.message ?: "\u5bfc\u51fa\u5931\u8d25" }, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importBackup(uri)
                Toast.makeText(context, result.getOrElse { it.message ?: "\u5bfc\u5165\u5931\u8d25" }, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    if (uiState.selectedTab == AppTab.TODAY) {
                        FloatingActionButton(
                            onClick = { editorDraft = HabitDraft() },
                            shape = RoundedCornerShape(22.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
                        ) {
                            PulseActionIcon(
                                kind = PulseIconKind.Add,
                                color = MaterialTheme.colorScheme.onPrimary,
                                compactLayout = LocalConfiguration.current.screenWidthDp <= 360,
                            )
                        }
                    }
                },
                bottomBar = {
                    PulseBottomBar(selectedTab = uiState.selectedTab, onSelect = viewModel::selectTab)
                },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    Crossfade(targetState = uiState.selectedTab, label = "tab") { tab ->
                        when (tab) {
                            AppTab.TODAY -> TodayScreen(
                                snapshot = uiState.todaySnapshot,
                                onAddHabit = { editorDraft = HabitDraft() },
                                onEditHabit = { habit -> editorDraft = HabitDraft.fromHabit(habit) },
                                onCheckInHabit = viewModel::checkInHabit,
                                onDeleteRecord = viewModel::deleteCheckInRecord,
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
                                notificationsGranted = notificationsGranted,
                                onThemeModeChange = viewModel::setThemeMode,
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
                        }
                    }
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
        Row(horizontalArrangement = Arrangement.spacedBy(itemSpacing), modifier = Modifier.fillMaxWidth()) {
            AppTab.values().forEach { tab ->
                val selected = tab == selectedTab
                val label = when (tab) {
                    AppTab.TODAY -> "\u4eca\u65e5"
                    AppTab.HISTORY -> "\u5386\u53f2"
                    AppTab.STATS -> "\u7edf\u8ba1"
                    AppTab.SETTINGS -> "\u8bbe\u7f6e"
                }
                val icon = when (tab) {
                    AppTab.TODAY -> PulseIconKind.TodayTab
                    AppTab.HISTORY -> PulseIconKind.HistoryTab
                    AppTab.STATS -> PulseIconKind.StatsTab
                    AppTab.SETTINGS -> PulseIconKind.SettingsTab
                }
                TextButton(
                    onClick = { onSelect(tab) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                            shape = RoundedCornerShape(18.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = if (compactLayout) 3.dp else 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        PulseActionIcon(
                            kind = icon,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            compactLayout = compactLayout,
                            modifier = Modifier.size(if (compactLayout) 30.dp else 32.dp),
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
