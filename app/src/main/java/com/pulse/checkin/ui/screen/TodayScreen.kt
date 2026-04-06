package com.pulse.checkin.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.TodayCheckInRecord
import com.pulse.checkin.domain.stats.TodayHabitSummary
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.PulsePrimaryActionButton
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.util.toPulseColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TodayScreen(
    snapshot: TodaySnapshot,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onCheckInHabit: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val bottomPadding = if (compactLayout) 84.dp else 92.dp

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "\u4eca\u65e5", compactLayout = compactLayout)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = contentSpacing,
                bottom = bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 12.dp)) {
                    OverviewMetric("\u4eca\u65e5\u6b21\u6570", snapshot.totalCount.toString(), Modifier.weight(1f), compactLayout)
                    OverviewMetric(
                        "\u5df2\u8fbe\u6807",
                        if (snapshot.targetHabitCount == 0) "0" else "${snapshot.completedHabits}/${snapshot.targetHabitCount}",
                        Modifier.weight(1f),
                        compactLayout,
                    )
                    OverviewMetric(
                        "\u6700\u4f73\u8fde\u51fb",
                        if (snapshot.bestCurrentStreak == 0) "--" else "${snapshot.bestCurrentStreak} \u5929",
                        Modifier.weight(1f),
                        compactLayout,
                    )
                }
            }
            if (snapshot.habits.isEmpty()) {
                item {
                    GlassCard {
                        Text("\u5148\u521b\u5efa\u4f60\u7684\u7b2c\u4e00\u4e2a\u4e60\u60ef", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\u6bcf\u4e2a\u4e60\u60ef\u73b0\u5728\u90fd\u53ef\u4ee5\u7528\u4e00\u4e2a\u6253\u5361\u6309\u94ae\u8ffd\u52a0\u8bb0\u5f55\uff0c\u5e76\u67e5\u770b\u5f53\u5929\u660e\u7ec6\u3002",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(if (compactLayout) 14.dp else 18.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PulsePrimaryActionButton(icon = PulseIconKind.Add, onClick = onAddHabit, compactLayout = compactLayout)
                        }
                    }
                }
            } else {
                items(snapshot.habits, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        compactLayout = compactLayout,
                        onEdit = { onEditHabit(item.habit) },
                        onCheckIn = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCheckInHabit(item.habit.id)
                        },
                        onDeleteRecord = onDeleteRecord,
                        onDeleteHabit = onDeleteHabit,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewMetric(title: String, value: String, modifier: Modifier = Modifier, compactLayout: Boolean) {
    GlassCard(modifier = modifier) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 12.dp))
        Text(
            value,
            style = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    item: TodayHabitSummary,
    compactLayout: Boolean,
    onEdit: () -> Unit,
    onCheckIn: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
) {
    var expanded by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    var pendingCheckIn by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    var pendingDeleteRecord by rememberSaveable(item.habit.id) { mutableStateOf<Long?>(null) }
    var pendingDeleteHabit by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    val progress = animateFloatAsState(targetValue = item.progress, label = "progress").value
    val cardShape = RoundedCornerShape(if (compactLayout) 24.dp else 28.dp)

    GlassCard(
        modifier = Modifier
            .animateContentSize()
            .clip(cardShape)
            .combinedClickable(onClick = {}, onLongClick = { pendingDeleteHabit = true }),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 46.dp else 52.dp)
                    .clip(CircleShape)
                    .background(item.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.habit.glyph, style = MaterialTheme.typography.titleLarge, color = item.habit.colorArgb.toPulseColor())
            }
            Spacer(modifier = Modifier.size(if (compactLayout) 10.dp else 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (item.habit.targetEnabled) "\u76ee\u6807 ${item.habit.dailyTargetCount ?: 1} \u6b21" else "\u4ec5\u8bb0\u5f55\u6b21\u6570",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PulseIconButton(kind = PulseIconKind.Edit, onClick = onEdit, compactLayout = compactLayout)
        }
        Spacer(modifier = Modifier.height(if (compactLayout) 14.dp else 18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = item.todayCount, label = "count") { count ->
                    Text(
                        count.toString(),
                        style = if (compactLayout) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val trailing = item.latestEventAt?.let {
                    val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                    "\u6700\u8fd1\u4e00\u6b21 ${time.format(timeFormatter)}"
                } ?: "\u4eca\u5929\u8fd8\u6ca1\u6709\u8bb0\u5f55"
                Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.size(12.dp))
            PulsePrimaryActionButton(
                onClick = { pendingCheckIn = true },
                compactLayout = compactLayout,
                label = if (!item.habit.targetEnabled || !item.reachedTarget) "\u6253\u5361" else null,
                icon = if (item.habit.targetEnabled && item.reachedTarget) PulseIconKind.Check else null,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Crossfade(targetState = item.habit.targetEnabled, label = "targetMode") { targetEnabled ->
            if (targetEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                    )
                    Text(
                        text = if (item.reachedTarget) {
                            "\u4eca\u65e5\u5df2\u8fbe\u6807"
                        } else {
                            "\u8ddd\u79bb\u8fbe\u6807\u8fd8\u5dee ${((item.habit.dailyTargetCount ?: 1) - item.todayCount).coerceAtLeast(0)} \u6b21"
                        },
                        color = if (item.reachedTarget) item.habit.colorArgb.toPulseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = if (item.todayCount == 0) "\u4eca\u5929\u8fd8\u6ca1\u6709\u5f00\u59cb\u8bb0\u5f55" else "\u53ef\u4ee5\u5c55\u5f00\u67e5\u770b\u4eca\u5929\u6240\u6709\u6253\u5361\u8bb0\u5f55",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (item.records.isEmpty()) "\u4eca\u5929\u6682\u65e0\u8bb0\u5f55" else "\u4eca\u65e5\u8bb0\u5f55 ${item.records.size} \u6761",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PulseIconButton(
                kind = if (expanded) PulseIconKind.Collapse else PulseIconKind.Records,
                onClick = { expanded = !expanded },
                enabled = item.records.isNotEmpty(),
                compactLayout = compactLayout,
                highlighted = expanded,
            )
        }
        if (expanded && item.records.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item.records.forEach { record ->
                    CheckInRecordRow(record = record, onDelete = { pendingDeleteRecord = record.id })
                }
            }
        }
    }

    if (pendingDeleteRecord != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            title = { Text("\u5220\u9664\u8bb0\u5f55") },
            text = { Text("\u786e\u5b9a\u5220\u9664\u8fd9\u6761\u6253\u5361\u8bb0\u5f55\u5417\uff1f") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteRecord?.let(onDeleteRecord)
                        pendingDeleteRecord = null
                    },
                ) {
                    Text("\u786e\u8ba4\u5220\u9664")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) {
                    Text("\u53d6\u6d88")
                }
            },
        )
    }

    if (pendingCheckIn) {
        AlertDialog(
            onDismissRequest = { pendingCheckIn = false },
            title = { Text("\u786e\u8ba4\u6253\u5361") },
            text = { Text("\u786e\u5b9a\u4e3a ${item.habit.name} \u65b0\u589e\u4e00\u6b21\u6253\u5361\u8bb0\u5f55\u5417\uff1f") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCheckIn()
                        pendingCheckIn = false
                    },
                ) {
                    Text("\u786e\u8ba4\u6253\u5361")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCheckIn = false }) {
                    Text("\u53d6\u6d88")
                }
            },
        )
    }

    if (pendingDeleteHabit) {
        AlertDialog(
            onDismissRequest = { pendingDeleteHabit = false },
            title = { Text("\u5220\u9664\u4e60\u60ef") },
            text = { Text("\u786e\u5b9a\u5220\u9664 ${item.habit.name} \u5417\uff1f\n\u5220\u9664\u540e\u5bf9\u5e94\u7684\u6253\u5361\u8bb0\u5f55\u4e5f\u4f1a\u4e00\u8d77\u9690\u85cf\u3002") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHabit(item.habit.id)
                        pendingDeleteHabit = false
                    },
                ) {
                    Text("\u786e\u8ba4\u5220\u9664")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteHabit = false }) {
                    Text("\u53d6\u6d88")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheckInRecordRow(
    record: TodayCheckInRecord,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .combinedClickable(onClick = {}, onLongClick = onDelete)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = record.displayTime.format(timeFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "\u957f\u6309\u540e\u786e\u8ba4\u5220\u9664",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}















