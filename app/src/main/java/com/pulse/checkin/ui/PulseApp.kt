package com.pulse.checkin.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HabitEditorSheet
import com.pulse.checkin.ui.screen.HistoryScreen
import com.pulse.checkin.ui.screen.SettingsScreen
import com.pulse.checkin.ui.screen.TodayScreen
import com.pulse.checkin.ui.theme.PulseTheme

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PulseApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editorDraft by remember { mutableStateOf<HabitDraft?>(null) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = checkNotificationsGranted(context)
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
                        FloatingActionButton(onClick = { editorDraft = HabitDraft() }, shape = RoundedCornerShape(22.dp)) {
                            Text("\u65b0\u5efa")
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
                                onIncrementHabit = viewModel::incrementHabit,
                                onDecrementHabit = viewModel::decrementHabit,
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                snapshot = uiState.historySnapshot,
                                selectedDate = uiState.selectedDate,
                                onPreviousMonth = { viewModel.shiftMonth(-1) },
                                onNextMonth = { viewModel.shiftMonth(1) },
                                onSelectDate = viewModel::selectDate,
                                onEditHabit = { habit -> editorDraft = HabitDraft.fromHabit(habit) },
                            )
                            AppTab.SETTINGS -> SettingsScreen(
                                themeMode = uiState.preferences.themeMode,
                                notificationsGranted = notificationsGranted,
                                onThemeModeChange = viewModel::setThemeMode,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
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
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    GlassCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTab.values().forEach { tab ->
                val selected = tab == selectedTab
                val label = when (tab) {
                    AppTab.TODAY -> "\u4eca\u65e5"
                    AppTab.HISTORY -> "\u5386\u53f2"
                    AppTab.SETTINGS -> "\u8bbe\u7f6e"
                }
                TextButton(
                    onClick = { onSelect(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                            shape = CircleShape,
                        ),
                ) {
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
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
