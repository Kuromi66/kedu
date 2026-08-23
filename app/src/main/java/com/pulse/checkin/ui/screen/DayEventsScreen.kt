package com.pulse.checkin.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.stats.DayCountCalculator
import com.pulse.checkin.domain.stats.DayCountResult
import com.pulse.checkin.ui.components.DestructiveConfirmDialog
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.i18n.PulseStrings
import java.time.LocalDate

@Composable
fun DayEventsScreen(
    dayEvents: List<DayEvent>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (DayEvent) -> Unit,
    onDelete: (String) -> Unit,
) {
    val strings = LocalPulseStrings.current
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val bottomPadding = if (compactLayout) 84.dp else 92.dp
    val today = LocalDate.now()
    val sorted = DayCountCalculator.sortForDisplay(dayEvents, today)
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.importantDatesTitle,
            compactLayout = compactLayout,
            leading = {
                PulseIconButton(
                    kind = PulseIconKind.ArrowLeft,
                    onClick = onBack,
                    compactLayout = compactLayout,
                )
            },
            action = {
                PulseIconButton(
                    kind = PulseIconKind.Add,
                    onClick = onAdd,
                    compactLayout = compactLayout,
                    highlighted = true,
                )
            },
        )
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
            if (sorted.isEmpty()) {
                item {
                    GlassCard {
                        Text(strings.noImportantDatesTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noImportantDatesDesc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                items(sorted, key = { it.id }) { event ->
                    DayEventCard(
                        event = event,
                        result = DayCountCalculator.compute(event, today),
                        strings = strings,
                        onClick = { onEdit(event) },
                        onDelete = { pendingDelete = event.id },
                    )
                }
            }
        }
    }

    pendingDelete?.let { id ->
        DestructiveConfirmDialog(
            message = strings.confirmDeleteDayEvent,
            confirmLabel = strings.confirmDelete,
            dismissLabel = strings.cancel,
            onConfirm = {
                onDelete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayEventCard(
    event: DayEvent,
    result: DayCountResult,
    strings: PulseStrings,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val badgeText = when (result) {
        is DayCountResult.DaysUntil -> strings.daysUntil(result.days.toInt())
        is DayCountResult.DaysSince -> strings.daysSince(result.days.toInt())
        DayCountResult.Today -> strings.dayEventToday
    }
    val isPast = result is DayCountResult.DaysSince
    val badgeColor = if (isPast) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val badgeBackground = if (isPast) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onDelete),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.dayEventDateText(event.date),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                event.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = badgeText,
                color = badgeColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
