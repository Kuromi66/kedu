package com.pulse.checkin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.CalendarDaySummary
import com.pulse.checkin.domain.stats.HistoryHabitDetail
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.util.toPulseColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74 M\u6708", Locale.CHINA)

@Composable
fun HistoryScreen(
    snapshot: MonthSnapshot,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onEditHabit: (Habit) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("History", style = MaterialTheme.typography.displaySmall)
                Text("\u4ece\u6708\u5386\u548c\u5f53\u65e5\u8be6\u60c5\u91cc\u770b\u89c1\u4f60\u7684\u79ef\u7d2f\u3002", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onPreviousMonth) { Text("\u4e0a\u6708") }
                    Text(snapshot.month.format(monthFormatter), style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = onNextMonth) { Text("\u4e0b\u6708") }
                }
                Spacer(modifier = Modifier.height(18.dp))
                CalendarGrid(month = snapshot.month, days = snapshot.calendarDays, selectedDate = selectedDate, onSelectDate = onSelectDate)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryMetric("\u6708\u6b21\u6570", snapshot.monthTotalCount.toString(), Modifier.weight(1f))
                HistoryMetric(
                    "\u8fbe\u6807\u7387",
                    if (snapshot.targetHabitDays == 0) "--" else "${(snapshot.completedHabitDays * 100 / snapshot.targetHabitDays)}%",
                    Modifier.weight(1f),
                )
                HistoryMetric(
                    "\u5f53\u524d\u8fde\u51fb",
                    if (snapshot.bestCurrentStreak == 0) "--" else "${snapshot.bestCurrentStreak} \u5929",
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Text(selectedDate.format(DateTimeFormatter.ofPattern("M\u6708d\u65e5 \u8be6\u60c5", Locale.CHINA)), style = MaterialTheme.typography.headlineMedium)
        }
        items(snapshot.selectedDateDetails, key = { it.habit.id }) { detail ->
            DayDetailCard(detail = detail, onEditHabit = onEditHabit)
        }
    }
}

@Composable
private fun HistoryMetric(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    days: List<CalendarDaySummary>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekHeaders = listOf("\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5")
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = List<LocalDate?>(offset) { null } + days.map { it.date }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weekHeaders.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { date ->
                    val summary = days.firstOrNull { it.date == date }
                    CalendarCell(
                        modifier = Modifier.weight(1f),
                        date = date,
                        summary = summary,
                        selected = date == selectedDate,
                        onClick = { if (date != null) onSelectDate(date) },
                    )
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    summary: CalendarDaySummary?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primary
        summary != null && summary.totalCount > 0 -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    }
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .clickable(enabled = date != null, onClick = onClick)
            .padding(8.dp),
    ) {
        if (date != null) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(date.dayOfMonth.toString(), color = contentColor, fontWeight = FontWeight.SemiBold)
                if ((summary?.totalCount ?: 0) > 0) {
                    Text("${summary?.totalCount} \u6b21", color = contentColor.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DayDetailCard(detail: HistoryHabitDetail, onEditHabit: (Habit) -> Unit) {
    GlassCard(modifier = Modifier.clickable { onEditHabit(detail.habit) }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(detail.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(detail.habit.glyph, color = detail.habit.colorArgb.toPulseColor(), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.size(12.dp))
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
            Text("${detail.count} \u6b21", style = MaterialTheme.typography.titleLarge)
        }
        if (detail.eventTimes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = detail.eventTimes.joinToString("  ") { it.toString().take(5) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


