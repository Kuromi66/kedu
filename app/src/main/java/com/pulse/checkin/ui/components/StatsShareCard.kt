package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.stats.MonthlyTrendPoint
import com.pulse.checkin.domain.stats.YearSnapshot
import com.pulse.checkin.ui.i18n.PulseStrings
import com.pulse.checkin.ui.util.toPulseColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun StatsShareCard(
    snapshot: YearSnapshot,
    strings: PulseStrings,
    modifier: Modifier = Modifier,
) {
    val habit = snapshot.selectedHabit
    val accent = habit?.colorArgb?.toPulseColor() ?: Color(0xFF2446FF)
    val textPrimary = Color(0xFF101828)
    val textSecondary = Color(0xFF667085)
    val tileSurface = Color(0xFFFFFFFF)
    val generatedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val metrics = snapshot.summaryMetrics
    val completionPercent = if (metrics.trackedDayCount > 0) {
        (metrics.completionDayCount * 100f / metrics.trackedDayCount).roundToInt()
    } else {
        0
    }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.22f),
                    0.35f to Color(0xFFF2F4F7),
                    1f to Color(0xFFF2F4F7),
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
                    text = habit?.name ?: strings.statsTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.yearText(snapshot.year),
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
            ShareMetricTile(
                label = strings.detailCompletion,
                value = "$completionPercent%",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
            ShareMetricTile(
                label = strings.activeDaysMetric,
                value = "${metrics.activeDayCount}",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShareMetricTile(
                label = strings.totalCheckInsMetric,
                value = "${metrics.totalCount}",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
            ShareMetricTile(
                label = strings.longestStreakMetric,
                value = "${metrics.longestStreak}",
                accent = accent,
                textSecondary = textSecondary,
                tileSurface = tileSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = strings.trendTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tileSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            ShareTrendChart(points = snapshot.trendPoints, accent = accent)
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
private fun ShareMetricTile(
    label: String,
    value: String,
    accent: Color,
    textSecondary: Color,
    tileSurface: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tileSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary,
        )
    }
}


@Composable
private fun ShareTrendChart(
    points: List<MonthlyTrendPoint>,
    accent: Color,
) {
    val maxCount = points.maxOfOrNull { it.totalCount } ?: 0
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barSpace = size.width / points.size.coerceAtLeast(1)
        val barWidth = barSpace * 0.5f
        val baseY = size.height
        points.forEachIndexed { index, point ->
            val fraction = if (maxCount == 0) 0f else point.totalCount.toFloat() / maxCount
            val barHeight = (size.height - 8f) * fraction.coerceIn(0f, 1f)
            val left = index * barSpace + (barSpace - barWidth) / 2f
            val highlighted = maxCount > 0 && point.totalCount == maxCount
            drawRoundRect(
                color = if (highlighted) accent else accent.copy(alpha = 0.28f),
                topLeft = Offset(left, baseY - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
