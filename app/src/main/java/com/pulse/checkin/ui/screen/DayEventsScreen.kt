package com.pulse.checkin.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pulse.checkin.domain.model.DayEvent
import com.pulse.checkin.domain.stats.DayCountCalculator
import com.pulse.checkin.domain.stats.DayCountResult
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.i18n.PulseStrings
import java.time.LocalDate
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayEventsScreen(
    dayEvents: List<DayEvent>,
    onAdd: () -> Unit,
    onEdit: (DayEvent) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val strings = LocalPulseStrings.current
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val bottomPadding = if (compactLayout) 84.dp else 92.dp
    val today = LocalDate.now()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var items by remember { mutableStateOf(dayEvents) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(dayEvents) {
        items = dayEvents
    }

    fun finishDrag() {
        if (draggingIndex != null) {
            val orderedIds = items.map { it.id }
            draggingIndex = null
            dragOffsetY = 0f
            if (orderedIds != dayEvents.map { it.id }) {
                onReorder(orderedIds)
            }
        }
    }

    fun moveItem(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        val currentIndex = draggingIndex ?: return
        dragOffsetY += dragAmount.y
        val layoutInfo = listState.layoutInfo
        val draggedInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex } ?: return
        val draggedCenterY = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY
        val targetIndex = layoutInfo.visibleItemsInfo
            .filter { it.index != currentIndex }
            .minByOrNull { abs((it.offset + it.size / 2f) - draggedCenterY) }
            ?.index
        if (targetIndex != null && targetIndex != currentIndex) {
            val newItems = items.toMutableList()
            val moved = newItems.removeAt(currentIndex)
            newItems.add(targetIndex, moved)
            items = newItems
            draggingIndex = targetIndex
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = strings.importantDatesTitle,
            compactLayout = compactLayout,
            action = {
                PulseIconButton(
                    kind = PulseIconKind.Add,
                    onClick = onAdd,
                    compactLayout = compactLayout,
                    highlighted = true,
                )
            },
        )
        if (items.isNotEmpty()) {
            Text(
                text = strings.reorderHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = contentSpacing,
                bottom = bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            if (items.isEmpty()) {
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
                itemsIndexed(items, key = { _, item -> item.id }) { index, event ->
                    val isDragging = draggingIndex == index
                    DayEventCard(
                        event = event,
                        result = DayCountCalculator.compute(event, today),
                        strings = strings,
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = if (isDragging) 1.02f else 1f
                                scaleY = if (isDragging) 1.02f else 1f
                            }
                            .animateItem(placementSpec = tween(160)),
                        onClick = { onEdit(event) },
                        onDragStart = {
                            draggingIndex = index
                            dragOffsetY = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, amount -> moveItem(change, amount) },
                        onDragEnd = { finishDrag() },
                        onDragCancel = { finishDrag() },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayEventCard(
    event: DayEvent,
    result: DayCountResult,
    strings: PulseStrings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
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
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(event.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            }
            .clickable(onClick = onClick),
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
            PulseActionIcon(
                kind = PulseIconKind.DragHandle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                compactLayout = true,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
