package com.pulse.checkin.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.TodayHabitSummary
import com.pulse.checkin.domain.stats.TodaySnapshot
import com.pulse.checkin.ui.components.GlassCard
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
    onIncrementHabit: (Long) -> Unit,
    onDecrementHabit: (Long) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Today", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "\u8f7b\u70b9\u4e00\u6b21\uff0c\u5c31\u628a\u4eca\u5929\u5411\u524d\u63a8\u4e00\u70b9\u3002",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewMetric("\u4eca\u65e5\u6b21\u6570", snapshot.totalCount.toString(), Modifier.weight(1f))
                OverviewMetric(
                    "\u5df2\u8fbe\u6807",
                    if (snapshot.targetHabitCount == 0) "0" else "${snapshot.completedHabits}/${snapshot.targetHabitCount}",
                    Modifier.weight(1f),
                )
                OverviewMetric(
                    "\u6700\u4f73\u8fde\u51fb",
                    if (snapshot.bestCurrentStreak == 0) "--" else "${snapshot.bestCurrentStreak} \u5929",
                    Modifier.weight(1f),
                )
            }
        }
        if (snapshot.habits.isEmpty()) {
            item {
                GlassCard {
                    Text("\u5148\u521b\u5efa\u4f60\u7684\u7b2c\u4e00\u4e2a\u4e60\u60ef", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\u652f\u6301\u4e00\u5929\u591a\u6b21\u6253\u5361\u3001\u53ef\u9009\u6bcf\u65e5\u76ee\u6807\u548c\u672c\u5730\u63d0\u9192\u3002",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(onClick = onAddHabit) { Text("\u5f00\u59cb\u6dfb\u52a0") }
                }
            }
        } else {
            items(snapshot.habits, key = { it.habit.id }) { item ->
                HabitCard(
                    item = item,
                    onEdit = { onEditHabit(item.habit) },
                    onIncrement = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIncrementHabit(item.habit.id)
                    },
                    onDecrement = { onDecrementHabit(item.habit.id) },
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun HabitCard(
    item: TodayHabitSummary,
    onEdit: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val progress = animateFloatAsState(targetValue = item.progress, label = "progress").value
    GlassCard(modifier = Modifier.animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(item.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.habit.glyph, style = MaterialTheme.typography.titleLarge, color = item.habit.colorArgb.toPulseColor())
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (item.habit.targetEnabled) "\u76ee\u6807 ${item.habit.dailyTargetCount ?: 1} \u6b21" else "\u4ec5\u8bb0\u5f55\u6b21\u6570",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onEdit) { Text("\u7f16\u8f91") }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                AnimatedContent(targetState = item.todayCount, label = "count") { count ->
                    Text(count.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                }
                val trailing = item.latestEventAt?.let {
                    val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                    "\u6700\u8fd1\u4e00\u6b21 ${time.format(timeFormatter)}"
                } ?: "\u4eca\u5929\u8fd8\u6ca1\u6709\u8bb0\u5f55"
                Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionBubble(label = "-1", onClick = onDecrement)
                ActionBubble(label = "+1", onClick = onIncrement, emphasized = true)
            }
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
                    text = if (item.todayCount == 0) "\u4eca\u5929\u8fd8\u6ca1\u6709\u5f00\u59cb\u8bb0\u5f55" else "\u7ee7\u7eed\u7d2f\u52a0\uff0c\u8ba9\u4eca\u5929\u66f4\u5b8c\u6574",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ActionBubble(label: String, onClick: () -> Unit, emphasized: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
