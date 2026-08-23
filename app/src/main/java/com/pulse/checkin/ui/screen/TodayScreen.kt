package com.pulse.checkin.ui.screen

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.TodayCheckInRecord
import com.pulse.checkin.domain.stats.TodayHabitSummary
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.ui.components.CheckInNoteDialog
import com.pulse.checkin.ui.components.DestructiveConfirmDialog
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HabitGlyph
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.PulsePrimaryActionButton
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.RecordNoteBadge
import com.pulse.checkin.ui.components.RecordDetailDialog
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.util.toPulseColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")


private fun formatElapsedSince(epochMillis: Long, strings: com.pulse.checkin.ui.i18n.PulseStrings): String {
    val elapsedMillis = max(0L, System.currentTimeMillis() - epochMillis)
    val elapsedMinutes = elapsedMillis / 60_000L
    return when {
        elapsedMinutes <= 0L -> strings.elapsedJustNow
        elapsedMinutes < 60L -> strings.elapsedMinutes(elapsedMinutes.toInt())
        elapsedMinutes < 1_440L -> strings.elapsedHours((elapsedMinutes / 60L).toInt())
        else -> strings.elapsedDays((elapsedMinutes / 1_440L).toInt())
    }
}

@Composable
fun TodayScreen(
    snapshot: TodaySnapshot,
    onEditHabit: (Habit) -> Unit,
    onCheckInHabit: (String, String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
) {
    val strings = LocalPulseStrings.current
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val bottomPadding = if (compactLayout) 84.dp else 92.dp

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = strings.todayTitle, compactLayout = compactLayout)
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
                    OverviewMetric(strings.todayCountMetric, snapshot.totalCount.toString(), Modifier.weight(1f), compactLayout)
                    OverviewMetric(
                        strings.reachedMetric,
                        if (snapshot.targetHabitCount == 0) "0" else "${snapshot.completedHabits}/${snapshot.targetHabitCount}",
                        Modifier.weight(1f),
                        compactLayout,
                    )
                    OverviewMetric(
                        strings.bestStreakMetric,
                        if (snapshot.bestCurrentStreak == 0) "--" else strings.streakDays(snapshot.bestCurrentStreak),
                        Modifier.weight(1f),
                        compactLayout,
                    )
                }
            }
            if (snapshot.habits.isEmpty()) {
                item {
                    GlassCard {
                        Text(strings.createFirstHabitTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.createFirstHabitDesc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(snapshot.habits, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        compactLayout = compactLayout,
                        onEdit = { onEditHabit(item.habit) },
                        onCheckInHabit = onCheckInHabit,
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
    GlassCard(modifier = modifier.heightIn(min = if (compactLayout) 108.dp else 116.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 12.dp))
        Text(
            text = value,
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
    onCheckInHabit: (String, String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    var pendingCheckIn by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    var pendingDeleteRecord by rememberSaveable(item.habit.id) { mutableStateOf<String?>(null) }
    var pendingDeleteHabit by rememberSaveable(item.habit.id) { mutableStateOf(false) }
    var pendingRecordDetail by remember(item.habit.id) { mutableStateOf<TodayCheckInRecord?>(null) }
    val strings = LocalPulseStrings.current
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
                HabitGlyph(
                    glyph = item.habit.glyph,
                    color = item.habit.colorArgb.toPulseColor(),
                    modifier = Modifier.size(if (compactLayout) 22.dp else 24.dp),
                    compactLayout = compactLayout,
                    textStyle = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.size(if (compactLayout) 10.dp else 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (item.habit.targetEnabled) strings.targetTimes(item.habit.dailyTargetCount ?: 1) else strings.recordOnly,
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
            val trailing = item.latestEventAt?.let {
                strings.latestCheckIn(formatElapsedSince(it, strings))
            } ?: strings.noCheckInToday
            Text(
                text = trailing,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.size(16.dp))
            PulsePrimaryActionButton(
                onClick = { pendingCheckIn = true },
                compactLayout = compactLayout,
                label = if (!item.habit.targetEnabled || !item.reachedTarget) strings.checkIn else null,
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
                            strings.reachedToday
                        } else {
                            strings.remainingToGoal(((item.habit.dailyTargetCount ?: 1) - item.todayCount).coerceAtLeast(0))
                        },
                        color = if (item.reachedTarget) item.habit.colorArgb.toPulseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else if (item.todayCount == 0) {
                Text(
                    text = strings.notStartedToday,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.records.isEmpty()) strings.noRecordsToday else strings.todayRecordsCount(item.records.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
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
                    CheckInRecordRow(
                        record = record,
                        onShowDetail = { pendingRecordDetail = record },
                        onDelete = { pendingDeleteRecord = record.id },
                    )
                }
            }
        }
    }

    if (pendingDeleteRecord != null) {
        DestructiveConfirmDialog(
            message = strings.confirmDeleteRecord,
            confirmLabel = strings.confirmDelete,
            dismissLabel = strings.cancel,
            onConfirm = {
                pendingDeleteRecord?.let(onDeleteRecord)
                pendingDeleteRecord = null
            },
            onDismiss = { pendingDeleteRecord = null },
        )
    }

    if (pendingCheckIn) {
        CheckInNoteDialog(
            title = strings.confirmCheckInTitle,
            message = strings.confirmCheckInText(item.habit.name),
            noteLabel = strings.noteLabel,
            notePlaceholder = strings.notePlaceholder,
            confirmLabel = strings.confirmCheckIn,
            dismissLabel = strings.cancel,
            onConfirm = { note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckInHabit(item.habit.id, note)
                pendingCheckIn = false
            },
            onDismiss = { pendingCheckIn = false },
        )
    }

    pendingRecordDetail?.let { record ->
        RecordDetailDialog(
            title = strings.recordDetailTitle,
            timeLabel = record.displayTime.format(timeFormatter),
            dateLabel = strings.historyDetailDate(LocalDate.now()),
            backfillBadge = if (record.isBackfilled) strings.backfilledRecord else null,
            note = record.note,
            noNoteLabel = strings.noNote,
            deleteLabel = strings.deleteRecordAction,
            dismissLabel = strings.cancel,
            onDelete = {
                pendingDeleteRecord = record.id
                pendingRecordDetail = null
            },
            onDismiss = { pendingRecordDetail = null },
        )
    }

    if (pendingDeleteHabit) {
        DestructiveConfirmDialog(
            message = strings.confirmDeleteHabit(item.habit.name),
            confirmLabel = strings.confirmDelete,
            dismissLabel = strings.cancel,
            onConfirm = {
                onDeleteHabit(item.habit.id)
                pendingDeleteHabit = false
            },
            onDismiss = { pendingDeleteHabit = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheckInRecordRow(
    record: TodayCheckInRecord,
    onShowDetail: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalPulseStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .combinedClickable(onClick = onShowDetail, onLongClick = onDelete)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = record.displayTime.format(timeFormatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!record.note.isNullOrBlank()) {
                RecordNoteBadge(text = strings.noteLabel)
            }
        }
        Text(
            text = strings.longPressDeleteHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}















