package com.pulse.checkin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.CalendarDaySummary
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.ui.i18n.PulseStrings
import com.pulse.checkin.ui.util.toPulseColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HistoryShareCard(
    snapshot: MonthSnapshot,
    habit: Habit?,
    strings: PulseStrings,
    modifier: Modifier = Modifier,
) {
    val accent = habit?.colorArgb?.toPulseColor() ?: Color(0xFF2446FF)
    val textPrimary = Color(0xFF101828)
    val textSecondary = Color(0xFF667085)
    val tileSurface = Color(0xFFFFFFFF)
    val cardBase = Color(0xFFF2F4F7)
    val topTint = lerp(cardBase, accent, 0.32f)
    val generatedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val maxCount = snapshot.calendarDays.maxOfOrNull { it.totalCount } ?: 0
    val completionPercent = if (snapshot.targetHabitDays > 0) {
        (snapshot.completedHabitDays * 100f / snapshot.targetHabitDays).roundToInt()
    } else {
        0
    }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0f to topTint,
                    0.35f to cardBase,
                    1f to cardBase,
                ),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitGlyph(
                    glyph = habit?.glyph ?: "P",
                    color = accent,
                    modifier = Modifier.size(24.dp),
                    compactLayout = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit?.name ?: strings.historyTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.historyMonthText(snapshot.month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                )
            }
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryShareMetricTile(
                label = strings.monthCountMetric,
                value = "${snapshot.monthTotalCount}",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
            HistoryShareMetricTile(
                label = strings.completionRateMetric,
                value = "$completionPercent%",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
            HistoryShareMetricTile(
                label = strings.currentStreakMetric,
                value = "${snapshot.bestCurrentStreak}",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = strings.historyTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tileSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            HistoryShareCalendar(
                snapshot = snapshot,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings,
                maxCount = maxCount,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${strings.appName} Pulse · $generatedAt",
            style = MaterialTheme.typography.labelMedium,
            color = textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HistoryShareMetricTile(
    label: String,
    value: String,
    accent: Color,
    textSecondary: Color,
    tileSurface: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tileSurface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryShareCalendar(
    snapshot: MonthSnapshot,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    strings: PulseStrings,
    maxCount: Int,
) {
    val today = LocalDate.now()
    val offset = snapshot.month.atDay(1).dayOfWeek.value - 1
    val cells: List<CalendarDaySummary?> =
        List(offset) { null } + snapshot.calendarDays

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            strings.weekHeaders.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { index ->
                    val cell = row.getOrNull(index)
                    val isToday = cell != null && cell.date == today
                    val fill = if (cell == null || cell.totalCount == 0) {
                        accent.copy(alpha = 0.06f)
                    } else {
                        val fraction = if (maxCount == 0) 0.35f else cell.totalCount.toFloat() / maxCount
                        accent.copy(alpha = 0.18f + 0.54f * fraction.coerceIn(0f, 1f))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (cell == null) Color.Transparent else fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null) {
                            Text(
                                text = if (isToday) strings.calendarTodayShort else "${cell.date.dayOfMonth}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isToday -> accent
                                    cell.totalCount > 0 -> textPrimary
                                    else -> textSecondary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
