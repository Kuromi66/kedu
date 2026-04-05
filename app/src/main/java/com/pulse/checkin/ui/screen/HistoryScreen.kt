package com.pulse.checkin.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.CalendarDaySummary
import com.pulse.checkin.domain.stats.CheckInRecordItem
import com.pulse.checkin.domain.stats.HistoryHabitDetail
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HeaderFilterButton
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.util.toPulseColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74 M\u6708", Locale.CHINA)
private val detailFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M\u6708d\u65e5 \u8be6\u60c5", Locale.CHINA)
private val recordTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HistoryScreen(
    snapshot: MonthSnapshot,
    habits: List<Habit>,
    selectedHabitId: Long?,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectHabit: (Long?) -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val currentMonth = YearMonth.now()
    var selectedDetailHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDetail = snapshot.selectedDateDetails.firstOrNull { it.habit.id == selectedDetailHabitId }
    var showFilters by rememberSaveable { mutableStateOf(selectedHabitId != null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "\u5386\u53f2",
            compactLayout = compactLayout,
            action = {
                HeaderFilterButton(
                    active = showFilters || selectedHabitId != null,
                    compactLayout = compactLayout,
                    onClick = { showFilters = !showFilters },
                )
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = contentSpacing,
                bottom = if (compactLayout) 108.dp else 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            item {
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(140)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("\u4e60\u60ef\u7b5b\u9009", style = MaterialTheme.typography.titleLarge)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
                            contentPadding = PaddingValues(end = 4.dp),
                        ) {
                            item {
                                HistoryFilterChip(
                                    label = "\u5168\u90e8",
                                    selected = selectedHabitId == null,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { onSelectHabit(null) },
                                )
                            }
                            items(habits, key = { it.id }) { habit ->
                                HistoryFilterChip(
                                    label = habit.name,
                                    selected = selectedHabitId == habit.id,
                                    accentColor = habit.colorArgb.toPulseColor(),
                                    prefix = habit.glyph,
                                    onClick = { onSelectHabit(habit.id) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                GlassCard(
                    modifier = Modifier.pointerInput(snapshot.month) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    totalDrag >= 48f -> onPreviousMonth()
                                    totalDrag <= -48f -> onNextMonth()
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = {
                                totalDrag = 0f
                            },
                        )
                    },
                ) {
                    AnimatedContent(
                        targetState = snapshot,
                        transitionSpec = {
                            val forward = targetState.month > initialState.month
                            (
                                slideInHorizontally(animationSpec = tween(280)) { fullWidth ->
                                    if (forward) fullWidth else -fullWidth / 3
                                } + fadeIn(animationSpec = tween(220))
                            ).togetherWith(
                                slideOutHorizontally(animationSpec = tween(280)) { fullWidth ->
                                    if (forward) -fullWidth / 3 else fullWidth
                                } + fadeOut(animationSpec = tween(180))
                            )
                        },
                        label = "monthChange",
                    ) { animatedSnapshot ->
                        MonthCalendarSection(
                            snapshot = animatedSnapshot,
                            selectedDate = selectedDate,
                            currentMonth = currentMonth,
                            compactLayout = compactLayout,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onBackToCurrentMonth = onBackToCurrentMonth,
                            onSelectDate = onSelectDate,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 12.dp)) {
                    HistoryMetric("\u6708\u6b21\u6570", snapshot.monthTotalCount.toString(), Modifier.weight(1f), compactLayout)
                    HistoryMetric(
                        "\u8fbe\u6807\u7387",
                        if (snapshot.targetHabitDays == 0) "--" else "${(snapshot.completedHabitDays * 100 / snapshot.targetHabitDays)}%",
                        Modifier.weight(1f),
                        compactLayout,
                    )
                    HistoryMetric(
                        "\u5f53\u524d\u8fde\u7eed",
                        if (snapshot.bestCurrentStreak == 0) "--" else "${snapshot.bestCurrentStreak} \u5929",
                        Modifier.weight(1f),
                        compactLayout,
                    )
                }
            }
            item {
                Text(selectedDate.format(detailFormatter), style = MaterialTheme.typography.headlineMedium)
            }
            if (snapshot.selectedDateDetails.isEmpty()) {
                item {
                    GlassCard {
                        Text("\u8fd9\u4e2a\u7b5b\u9009\u6761\u4ef6\u4e0b\u6682\u65e0\u8bb0\u5f55", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(snapshot.selectedDateDetails, key = { it.habit.id }) { detail ->
                    DayDetailCard(detail = detail, compactLayout = compactLayout, onClick = { selectedDetailHabitId = detail.habit.id })
                }
            }
        }
    }

    selectedDetail?.let { detail ->
        HistoryRecordSheet(
            detail = detail,
            compactLayout = compactLayout,
            onDismiss = { selectedDetailHabitId = null },
            onDeleteRecord = onDeleteRecord,
        )
    }
}

@Composable
private fun MonthCalendarSection(
    snapshot: MonthSnapshot,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    compactLayout: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 14.dp else 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthSwitchButton(
                symbol = "\u2039",
                compactLayout = compactLayout,
                onClick = onPreviousMonth,
            )
            Text(
                text = snapshot.month.format(monthFormatter),
                style = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MonthSwitchButton(
                    symbol = "\u25ce",
                    compactLayout = compactLayout,
                    enabled = snapshot.month != currentMonth,
                    onClick = onBackToCurrentMonth,
                )
                MonthSwitchButton(
                    symbol = "\u203a",
                    compactLayout = compactLayout,
                    onClick = onNextMonth,
                )
            }
        }
        CalendarGrid(
            month = snapshot.month,
            days = snapshot.calendarDays,
            selectedDate = selectedDate,
            onSelectDate = onSelectDate,
            compactLayout = compactLayout,
        )
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    prefix: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White.copy(alpha = 0.24f) else accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else accentColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(label, fontWeight = FontWeight.Medium)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            selectedBorderColor = accentColor,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
        ),
    )
}

@Composable
private fun HistoryMetric(title: String, value: String, modifier: Modifier = Modifier, compactLayout: Boolean) {
    GlassCard(modifier = modifier) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 10.dp))
        Text(value, style = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun MonthSwitchButton(
    symbol: String,
    compactLayout: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(if (compactLayout) 34.dp else 38.dp),
    ) {
        Text(
            text = symbol,
            style = if (compactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    days: List<CalendarDaySummary>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    compactLayout: Boolean,
) {
    val weekHeaders = listOf("\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5")
    val offset = month.atDay(1).dayOfWeek.value - 1
    val summaryByDate = days.associateBy { it.date }
    val cells = List<LocalDate?>(offset) { null } + days.map { it.date }
    val horizontalSpacing = if (compactLayout) 4.dp else 8.dp
    val maxCount = days.maxOfOrNull { it.totalCount } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
            weekHeaders.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = if (compactLayout) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                week.forEach { date ->
                    CalendarCell(
                        modifier = Modifier.weight(1f),
                        date = date,
                        summary = date?.let(summaryByDate::get),
                        maxCount = maxCount,
                        selected = date == selectedDate,
                        onClick = { if (date != null) onSelectDate(date) },
                        compactLayout = compactLayout,
                    )
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    summary: CalendarDaySummary?,
    maxCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    compactLayout: Boolean,
) {
    val density = if ((summary?.totalCount ?: 0) > 0 && maxCount > 0) {
        summary!!.totalCount.toFloat() / maxCount.toFloat()
    } else {
        0f
    }
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primary
        density > 0f -> lerp(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            density.coerceIn(0f, 1f),
        )
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        density > 0.58f -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
             .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = date != null, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = if (compactLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DayDetailCard(detail: HistoryHabitDetail, compactLayout: Boolean, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 40.dp else 44.dp)
                    .clip(CircleShape)
                    .background(detail.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(detail.habit.glyph, color = detail.habit.colorArgb.toPulseColor(), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.size(if (compactLayout) 10.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.habit.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (detail.habit.targetEnabled) {
                        if (detail.reachedTarget) "\u5f53\u65e5\u5df2\u8fbe\u6807" else "\u76ee\u6807 ${detail.habit.dailyTargetCount ?: 1} \u6b21"
                    } else {
                        "\u4ec5\u8bb0\u5f55\u6b21\u6570"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${detail.count} \u6b21",
                style = if (compactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRecordSheet(
    detail: HistoryHabitDetail,
    compactLayout: Boolean,
    onDismiss: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
) {
    var pendingDeleteRecord by remember(detail.habit.id) { mutableStateOf<CheckInRecordItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactLayout) 16.dp else 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactLayout) 12.dp else 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (compactLayout) 42.dp else 48.dp)
                        .clip(CircleShape)
                        .background(detail.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = detail.habit.glyph,
                        color = detail.habit.colorArgb.toPulseColor(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.habit.name, style = if (compactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (detail.count == 0) {
                            "\u5f53\u65e5\u6682\u65e0\u6253\u5361\u8bb0\u5f55"
                        } else {
                            "\u5171 ${detail.count} \u6b21\u6253\u5361\uff0c\u957f\u6309\u53ef\u5220\u9664\u5355\u6761"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (detail.records.isEmpty()) {
                GlassCard {
                    Text("\u8fd9\u4e00\u5929\u8fd8\u6ca1\u6709\u4efb\u4f55\u6253\u5361\u8bb0\u5f55", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    detail.records.forEachIndexed { index, record ->
                        HistoryRecordRow(
                            index = index,
                            record = record,
                            compactLayout = compactLayout,
                            onDelete = { pendingDeleteRecord = record },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            title = { Text("\u5220\u9664\u8bb0\u5f55") },
            text = { Text("\u786e\u5b9a\u5220\u9664 ${record.displayTime.format(recordTimeFormatter)} \u8fd9\u6761\u6253\u5361\u8bb0\u5f55\u5417\uff1f") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(record.id)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRecordRow(
    index: Int,
    record: CheckInRecordItem,
    compactLayout: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .combinedClickable(onClick = {}, onLongClick = onDelete)
            .padding(horizontal = if (compactLayout) 14.dp else 16.dp, vertical = if (compactLayout) 12.dp else 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text("\u7b2c ${index + 1} \u6b21", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("\u957f\u6309\u540e\u786e\u8ba4\u5220\u9664", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(record.displayTime.format(recordTimeFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}


















