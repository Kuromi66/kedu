package com.pulse.checkin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.model.Habit
import com.pulse.checkin.domain.stats.CalendarDaySummary
import com.pulse.checkin.domain.stats.CheckInRecordItem
import com.pulse.checkin.domain.stats.HistoryHabitDetail
import com.pulse.checkin.domain.stats.MonthSnapshot
import com.pulse.checkin.ui.components.CheckInNoteDialog
import com.pulse.checkin.ui.components.DestructiveConfirmDialog
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HistoryShareCard
import com.pulse.checkin.ui.components.HabitGlyph
import com.pulse.checkin.ui.components.HeaderFilterButton
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.RecordNoteBadge
import com.pulse.checkin.ui.components.RecordDetailDialog
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.components.ShareImageDialog
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.util.showPulseTimePicker
import com.pulse.checkin.ui.util.saveBitmapToGallery
import com.pulse.checkin.ui.util.shareBitmap
import com.pulse.checkin.ui.util.toPulseColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val recordTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HistoryScreen(
    snapshot: MonthSnapshot,
    habits: List<Habit>,
    selectedHabitId: String?,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectHabit: (String?) -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onBackfillHabit: (String, LocalDate, String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onUpdateRecordTime: (String, Long) -> Unit,
) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val strings = LocalPulseStrings.current
    val context = LocalContext.current
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    val currentMonth = YearMonth.now()
    val canGoToNextMonth = snapshot.month < currentMonth
    var selectedDetailHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDetail = snapshot.selectedDateDetails.firstOrNull { it.habit.id == selectedDetailHabitId }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val shareLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val shareHabit = habits.firstOrNull { it.id == selectedHabitId } ?: habits.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = strings.historyTitle,
                compactLayout = compactLayout,
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PulseIconButton(
                            kind = PulseIconKind.Share,
                            compactLayout = compactLayout,
                            onClick = {
                                coroutineScope.launch {
                                    shareBitmap = runCatching {
                                        shareLayer.toImageBitmap().asAndroidBitmap()
                                    }.getOrNull()
                                    showShareOptions = shareBitmap != null
                                }
                            },
                        )
                        HeaderFilterButton(
                            active = showFilters,
                            compactLayout = compactLayout,
                            onClick = { showFilters = !showFilters },
                        )
                    }
                },
            )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = contentSpacing,
                bottom = if (compactLayout) 84.dp else 92.dp,
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
                        Text(strings.habitFilterTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
                            contentPadding = PaddingValues(end = 4.dp),
                        ) {
                            items(habits, key = { it.id }) { habit ->
                                HistoryFilterChip(
                                    label = habit.name,
                                    selected = selectedHabitId == habit.id,
                                    accentColor = habit.colorArgb.toPulseColor(),
                                    glyph = habit.glyph,
                                    onClick = { onSelectHabit(habit.id) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                GlassCard {
                    MonthCalendarSection(
                        snapshot = snapshot,
                        selectedDate = selectedDate,
                        currentMonth = currentMonth,
                        compactLayout = compactLayout,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        canGoToNextMonth = canGoToNextMonth,
                        onBackToCurrentMonth = onBackToCurrentMonth,
                        onSelectDate = onSelectDate,
                    )
                }
            }
            item {
                HistoryOverviewCard(snapshot = snapshot, compactLayout = compactLayout)
            }
            item {
                Text(strings.historyDetailDate(selectedDate), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            if (snapshot.selectedDateDetails.isEmpty()) {
                item {
                    GlassCard {
                        Text(strings.noRecordsForFilter, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(snapshot.selectedDateDetails, key = { it.habit.id }) { detail ->
                    DayDetailCard(detail = detail, compactLayout = compactLayout, onClick = { selectedDetailHabitId = detail.habit.id })
                }
            }
            }
        }

        Box(
            modifier = Modifier
                .size(480.dp, 550.dp)
                .drawWithContent {
                    shareLayer.record { this@drawWithContent.drawContent() }
                },
        ) {
            HistoryShareCard(snapshot = snapshot, habit = shareHabit, strings = strings)
        }
    }

    if (showShareOptions) {
        ShareImageDialog(
            title = strings.share,
            description = strings.shareHistoryDesc,
            bitmap = shareBitmap,
            saveLabel = strings.saveToGallery,
            shareLabel = strings.share,
            cancelLabel = strings.cancel,
            onSave = {
                val bitmap = shareBitmap
                showShareOptions = false
                if (bitmap != null) {
                    val saved = saveBitmapToGallery(context, bitmap)
                    Toast.makeText(context, if (saved) strings.saveSuccess else strings.saveFailed, Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                val bitmap = shareBitmap
                showShareOptions = false
                if (bitmap != null) {
                    shareBitmap(context, bitmap, strings.share)
                }
            },
            onDismiss = { showShareOptions = false },
        )
    }

    selectedDetail?.let { detail ->
        HistoryRecordSheet(
            detail = detail,
            selectedDate = selectedDate,
            compactLayout = compactLayout,
            onDismiss = { selectedDetailHabitId = null },
            onBackfillHabit = onBackfillHabit,
            onDeleteRecord = onDeleteRecord,
            onUpdateRecordTime = onUpdateRecordTime,
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
    canGoToNextMonth: Boolean,
    onBackToCurrentMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val strings = LocalPulseStrings.current
    val scope = rememberCoroutineScope()
    var dragOffsetPx by remember(snapshot.month) { mutableFloatStateOf(0f) }
    var pageWidthPx by remember { mutableFloatStateOf(0f) }

    fun animateMonthSwitch(targetOffsetSign: Float, onComplete: () -> Unit) {
        val widthPx = pageWidthPx
        if (widthPx <= 0f) {
            onComplete()
            return
        }
        scope.launch {
            animate(
                initialValue = dragOffsetPx,
                targetValue = widthPx * targetOffsetSign,
                animationSpec = tween(180),
            ) { value, _ ->
                dragOffsetPx = value
            }
            dragOffsetPx = 0f
            onComplete()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 14.dp else 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthSwitchButton(
                type = MonthSwitchType.Previous,
                compactLayout = compactLayout,
                onClick = { animateMonthSwitch(1f, onPreviousMonth) },
            )
            Text(
                text = strings.historyMonthText(snapshot.month),
                style = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MonthSwitchButton(
                    type = MonthSwitchType.Current,
                    compactLayout = compactLayout,
                    enabled = snapshot.month != currentMonth,
                    onClick = {
                        val direction = if (snapshot.month < currentMonth) -1f else 1f
                        animateMonthSwitch(direction, onBackToCurrentMonth)
                    },
                )
                MonthSwitchButton(
                    type = MonthSwitchType.Next,
                    compactLayout = compactLayout,
                    enabled = canGoToNextMonth,
                    onClick = { animateMonthSwitch(-1f, onNextMonth) },
                )
            }
        }
        CalendarWeekHeader(compactLayout = compactLayout)
        LaunchedEffect(snapshot.month) {
            dragOffsetPx = 0f
        }
        val previousMonth = snapshot.month.minusMonths(1)
        val nextMonth = snapshot.month.plusMonths(1)
        val density = LocalDensity.current
        val horizontalSpacingPx = with(density) { (if (compactLayout) 4.dp else 8.dp).toPx() }
        val verticalSpacingPx = with(density) { (if (compactLayout) 8.dp else 10.dp).toPx() }
        val bottomInsetPx = with(density) { (if (compactLayout) 4.dp else 6.dp).toPx() }
        val currentRows = remember(snapshot.month) { calendarRowCount(snapshot.month) }
        val previousRows = remember(previousMonth) { calendarRowCount(previousMonth) }
        val nextRows = remember(nextMonth) { calendarRowCount(nextMonth) }
        val currentHeightPx = remember(pageWidthPx, currentRows, horizontalSpacingPx, verticalSpacingPx) {
            calculateCalendarGridHeightPx(pageWidthPx, currentRows, horizontalSpacingPx, verticalSpacingPx) + bottomInsetPx
        }
        val dragProgress = if (pageWidthPx > 0f) (abs(dragOffsetPx) / pageWidthPx).coerceIn(0f, 1f) else 0f
        val targetRows = when {
            dragOffsetPx > 0f -> previousRows
            dragOffsetPx < 0f && canGoToNextMonth -> nextRows
            else -> currentRows
        }
        val targetHeightPx = remember(pageWidthPx, targetRows, horizontalSpacingPx, verticalSpacingPx) {
            calculateCalendarGridHeightPx(pageWidthPx, targetRows, horizontalSpacingPx, verticalSpacingPx) + bottomInsetPx
        }
        val containerHeightPx = if (dragProgress == 0f) {
            currentHeightPx
        } else {
            currentHeightPx + (targetHeightPx - currentHeightPx) * dragProgress
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (containerHeightPx > 0f) Modifier.height(with(density) { containerHeightPx.toDp() })
                    else Modifier
                )
                .clipToBounds()
                .onSizeChanged { pageWidthPx = it.width.toFloat() }
                .pointerInput(snapshot.month, canGoToNextMonth) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val widthPx = if (pageWidthPx > 0f) pageWidthPx else size.width.toFloat()
                            val minOffset = if (canGoToNextMonth) -widthPx else 0f
                            val maxOffset = widthPx
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(minOffset, maxOffset)
                        },
                        onDragEnd = {
                            val widthPx = if (pageWidthPx > 0f) pageWidthPx else size.width.toFloat()
                            val threshold = widthPx * 0.25f
                            when {
                                dragOffsetPx >= threshold -> {
                                    scope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = widthPx,
                                            animationSpec = tween(180),
                                        ) { value, _ ->
                                            dragOffsetPx = value
                                        }
                                        dragOffsetPx = 0f
                                        onPreviousMonth()
                                    }
                                }

                                dragOffsetPx <= -threshold && canGoToNextMonth -> {
                                    scope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = -widthPx,
                                            animationSpec = tween(180),
                                        ) { value, _ ->
                                            dragOffsetPx = value
                                        }
                                        dragOffsetPx = 0f
                                        onNextMonth()
                                    }
                                }

                                else -> {
                                    scope.launch {
                                        animate(
                                            initialValue = dragOffsetPx,
                                            targetValue = 0f,
                                            animationSpec = tween(220),
                                        ) { value, _ ->
                                            dragOffsetPx = value
                                        }
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(
                                    initialValue = dragOffsetPx,
                                    targetValue = 0f,
                                    animationSpec = tween(220),
                                ) { value, _ ->
                                    dragOffsetPx = value
                                }
                            }
                        },
                    )
                },
        ) {
            CalendarDateGrid(
                month = snapshot.month,
                days = snapshot.calendarDays,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                compactLayout = compactLayout,
                interactive = true,
                previewMode = false,
                modifier = Modifier.offset { IntOffset(dragOffsetPx.roundToInt(), 0) },
            )

            if (pageWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((dragOffsetPx - pageWidthPx).roundToInt(), 0) },
                ) {
                    CalendarDateGrid(
                        month = previousMonth,
                        days = emptyList(),
                        selectedDate = selectedDate,
                        onSelectDate = {},
                        compactLayout = compactLayout,
                        interactive = false,
                        previewMode = true,
                    )
                }

                if (canGoToNextMonth) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((dragOffsetPx + pageWidthPx).roundToInt(), 0) },
                    ) {
                        CalendarDateGrid(
                            month = nextMonth,
                            days = emptyList(),
                            selectedDate = selectedDate,
                            onSelectDate = {},
                            compactLayout = compactLayout,
                            interactive = false,
                            previewMode = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    glyph: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (glyph != null) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White.copy(alpha = 0.24f) else accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        HabitGlyph(
                            glyph = glyph,
                            color = if (selected) Color.White else accentColor,
                            modifier = Modifier.size(12.dp),
                            compactLayout = true,
                            textStyle = MaterialTheme.typography.labelMedium,
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
private fun HistoryOverviewCard(snapshot: MonthSnapshot, compactLayout: Boolean) {
    val strings = LocalPulseStrings.current
    val completionProgress = if (snapshot.targetHabitDays == 0) {
        0f
    } else {
        snapshot.completedHabitDays.toFloat() / snapshot.targetHabitDays.toFloat()
    }.coerceIn(0f, 1f)
    val completionPercent = (completionProgress * 100f).toInt()
    val animatedCompletionProgress by animateFloatAsState(
        targetValue = completionProgress,
        animationSpec = tween(durationMillis = 420),
        label = "history-completion-progress",
    )
    val animatedCompletionPercent by animateIntAsState(
        targetValue = completionPercent,
        animationSpec = tween(durationMillis = 320),
        label = "history-completion-percent",
    )
    val activeDays = snapshot.calendarDays.count { it.totalCount > 0 }
    val ringSize = if (compactLayout) 74.dp else 84.dp
    val ringStroke = if (compactLayout) 9.dp else 11.dp
    val metricValueStyle = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium
    val metricLabelStyle = if (compactLayout) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val ringTextStyle = if (compactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    val ringProgressColor = MaterialTheme.colorScheme.primary
    val metricLabelTopPadding = if (compactLayout) 8.dp else 10.dp

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 18.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = ringTrackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = ringStroke.toPx(), cap = StrokeCap.Round),
                    )
                    if (completionProgress > 0f) {
                        drawArc(
                            color = ringProgressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedCompletionProgress,
                            useCenter = false,
                            style = Stroke(width = ringStroke.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$animatedCompletionPercent%",
                        style = ringTextStyle,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.completionRateMetric,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistoryOverviewMetric(
                    value = snapshot.monthTotalCount.toString(),
                    label = strings.monthCountMetric,
                    valueStyle = metricValueStyle,
                    labelStyle = metricLabelStyle,
                    modifier = Modifier.weight(1f),
                    labelTopPadding = metricLabelTopPadding,
                )
                HistoryOverviewMetric(
                    value = activeDays.toString(),
                    label = strings.activeDaysMetric,
                    valueStyle = metricValueStyle,
                    labelStyle = metricLabelStyle,
                    modifier = Modifier.weight(1f),
                    labelTopPadding = metricLabelTopPadding,
                )
                HistoryOverviewMetric(
                    value = snapshot.bestCurrentStreak.toString(),
                    label = strings.currentStreakMetric,
                    valueStyle = metricValueStyle,
                    labelStyle = metricLabelStyle,
                    modifier = Modifier.weight(1f),
                    labelTopPadding = metricLabelTopPadding,
                )
            }
        }
    }
}

@Composable
private fun HistoryOverviewMetric(
    value: String,
    label: String,
    valueStyle: androidx.compose.ui.text.TextStyle,
    labelStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    labelTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = value, style = valueStyle, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = labelStyle,
            modifier = Modifier.padding(top = labelTopPadding),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
    }
}

private enum class MonthSwitchType {
    Previous,
    Current,
    Next,
}

@Composable
private fun MonthSwitchButton(
    type: MonthSwitchType,
    compactLayout: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val buttonSize = if (compactLayout) 40.dp else 44.dp
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val iconKind = when (type) {
        MonthSwitchType.Previous -> PulseIconKind.ArrowLeft
        MonthSwitchType.Next -> PulseIconKind.ArrowRight
        MonthSwitchType.Current -> PulseIconKind.CurrentTime
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(buttonSize),
    ) {
        PulseActionIcon(
            kind = iconKind,
            color = iconColor,
            compactLayout = compactLayout,
            modifier = Modifier.size(if (compactLayout) 22.dp else 24.dp),
        )
    }
}

@Composable
private fun CalendarWeekHeader(compactLayout: Boolean) {
    val weekHeaders = LocalPulseStrings.current.weekHeaders
    val horizontalSpacing = if (compactLayout) 4.dp else 8.dp

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
}

@Composable
private fun CalendarDateGrid(
    month: YearMonth,
    days: List<CalendarDaySummary>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    compactLayout: Boolean,
    interactive: Boolean,
    previewMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val summaryByDate = days.associateBy { it.date }
    val cells = remember(month) { buildCalendarPageCells(month) }
    val horizontalSpacing = if (compactLayout) 4.dp else 8.dp
    val maxCount = days.maxOfOrNull { it.totalCount } ?: 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
    ) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        CalendarCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            summary = summaryByDate[date],
                            maxCount = maxCount,
                            selected = interactive && date == selectedDate,
                            enabled = interactive,
                            compactLayout = compactLayout,
                            previewMode = previewMode,
                            onClick = { onSelectDate(date) },
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

private fun buildCalendarPageCells(month: YearMonth): List<LocalDate?> {
    val offset = month.atDay(1).dayOfWeek.value - 1
    return List<LocalDate?>(offset) { null } + (1..month.lengthOfMonth()).map { day -> month.atDay(day) }
}

private fun calendarRowCount(month: YearMonth): Int {
    val offset = month.atDay(1).dayOfWeek.value - 1
    return (offset + month.lengthOfMonth() + 6) / 7
}

private fun calculateCalendarGridHeightPx(
    pageWidthPx: Float,
    rowCount: Int,
    horizontalSpacingPx: Float,
    verticalSpacingPx: Float,
): Float {
    if (pageWidthPx <= 0f || rowCount <= 0) return 0f
    val cellSizePx = (pageWidthPx - horizontalSpacingPx * 6f) / 7f
    return cellSizePx * rowCount + verticalSpacingPx * (rowCount - 1)
}

@Composable
private fun CalendarCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    summary: CalendarDaySummary?,
    maxCount: Int,
    selected: Boolean,
    enabled: Boolean,
    compactLayout: Boolean,
    previewMode: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalPulseStrings.current
    val isToday = date == LocalDate.now()
    val dayLabel = if (isToday) strings.calendarTodayShort else date.dayOfMonth.toString()
    val density = if ((summary?.totalCount ?: 0) > 0 && maxCount > 0) {
        summary!!.totalCount.toFloat() / maxCount.toFloat()
    } else {
        0f
    }
    val selectedBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val backgroundColor = when {
        selected -> selectedBackground
        previewMode -> Color.Transparent
        density > 0f -> lerp(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            density.coerceIn(0f, 1f),
        )
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onTertiary
        previewMode -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
        density > 0.58f -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 240),
        label = "calendarCellBackground",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = 220),
        label = "calendarCellContent",
    )
    val backfillMarkerColor by animateColorAsState(
        targetValue = if (selected || density > 0.58f) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 220),
        label = "calendarCellMarker",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(animatedBackgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayLabel,
            color = animatedContentColor,
            fontWeight = if (previewMode) FontWeight.Medium else FontWeight.SemiBold,
            style = if (compactLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        if (!previewMode && summary?.hasBackfilledRecord == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compactLayout) 2.dp else 3.dp)
                    .size(if (compactLayout) 4.dp else 5.dp)
                    .clip(CircleShape)
                    .background(backfillMarkerColor),
            )
        }
    }
}

@Composable
private fun DayDetailCard(detail: HistoryHabitDetail, compactLayout: Boolean, onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(if (compactLayout) 24.dp else 28.dp)
    val strings = LocalPulseStrings.current
    GlassCard(modifier = Modifier.clip(cardShape).clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 40.dp else 44.dp)
                    .clip(CircleShape)
                    .background(detail.habit.colorArgb.toPulseColor().copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                HabitGlyph(
                    glyph = detail.habit.glyph,
                    color = detail.habit.colorArgb.toPulseColor(),
                    modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp),
                    compactLayout = compactLayout,
                )
            }
            Spacer(modifier = Modifier.size(if (compactLayout) 10.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.habit.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (detail.habit.targetEnabled) {
                        if (detail.reachedTarget) strings.dayReached else strings.targetTimes(detail.habit.dailyTargetCount ?: 1)
                    } else {
                        strings.recordOnly
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = strings.countTimes(detail.count),
                style = if (compactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRecordSheet(
    detail: HistoryHabitDetail,
    selectedDate: LocalDate,
    compactLayout: Boolean,
    onDismiss: () -> Unit,
    onBackfillHabit: (String, LocalDate, String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onUpdateRecordTime: (String, Long) -> Unit,
) {
    val context = LocalContext.current
    var pendingDeleteRecord by remember(detail.habit.id, selectedDate) { mutableStateOf<CheckInRecordItem?>(null) }
    var pendingBackfill by remember(detail.habit.id, selectedDate) { mutableStateOf(false) }
    var pendingRecordDetail by remember(detail.habit.id, selectedDate) { mutableStateOf<CheckInRecordItem?>(null) }
    val strings = LocalPulseStrings.current
    val canBackfill = selectedDate.isBefore(LocalDate.now())

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
                    HabitGlyph(
                        glyph = detail.habit.glyph,
                        color = detail.habit.colorArgb.toPulseColor(),
                        modifier = Modifier.size(if (compactLayout) 20.dp else 22.dp),
                        compactLayout = compactLayout,
                        textStyle = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.habit.name, style = if (compactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (detail.count == 0) {
                            strings.noDayRecords
                        } else {
                            strings.totalCheckIns(detail.count)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (canBackfill) {
                    TextButton(onClick = { pendingBackfill = true }) {
                        Text(
                            text = strings.backfill,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (detail.records.isEmpty()) {
                GlassCard {
                    Text(strings.noDayRecords, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    detail.records.forEachIndexed { index, record ->
                        HistoryRecordRow(
                            index = detail.records.size - 1 - index,
                            record = record,
                            compactLayout = compactLayout,
                            onShowDetail = { pendingRecordDetail = record },
                            onDelete = { pendingDeleteRecord = record },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteRecord?.let { record ->
        DestructiveConfirmDialog(
            message = strings.deleteRecordAt(record.displayTime.format(recordTimeFormatter)),
            confirmLabel = strings.confirmDelete,
            dismissLabel = strings.cancel,
            onConfirm = {
                onDeleteRecord(record.id)
                pendingDeleteRecord = null
            },
            onDismiss = { pendingDeleteRecord = null },
        )
    }

    if (pendingBackfill) {
        CheckInNoteDialog(
            title = strings.confirmBackfill,
            message = strings.confirmBackfillText(strings.historyDetailDate(selectedDate), detail.habit.name),
            noteLabel = strings.noteLabel,
            notePlaceholder = strings.notePlaceholder,
            confirmLabel = strings.confirmBackfill,
            dismissLabel = strings.cancel,
            onConfirm = { note ->
                onBackfillHabit(detail.habit.id, selectedDate, note)
                pendingBackfill = false
            },
            onDismiss = { pendingBackfill = false },
        )
    }

    pendingRecordDetail?.let { record ->
        val timeZone = ZoneId.systemDefault()
        RecordDetailDialog(
            title = strings.recordDetailTitle,
            timeLabel = record.displayTime.format(recordTimeFormatter),
            dateLabel = strings.historyDetailDate(selectedDate),
            backfillBadge = if (record.isBackfilled) strings.backfilledRecord else null,
            note = record.note,
            noNoteLabel = strings.noNote,
            editTimeLabel = strings.editTime,
            deleteLabel = strings.deleteRecordAction,
            dismissLabel = strings.cancel,
            onEditTime = {
                showPulseTimePicker(
                    context = context,
                    initialHour = record.displayTime.hour,
                    initialMinute = record.displayTime.minute,
                    onSelected = { hour, minute ->
                        val date = Instant.ofEpochMilli(record.occurredAtEpochMillis)
                            .atZone(timeZone)
                            .toLocalDate()
                        val newMillis = date.atTime(hour, minute)
                            .atZone(timeZone)
                            .toInstant()
                            .toEpochMilli()
                        onUpdateRecordTime(record.id, newMillis)
                        pendingRecordDetail = null
                    },
                )
            },
            onDelete = {
                pendingDeleteRecord = record
                pendingRecordDetail = null
            },
            onDismiss = { pendingRecordDetail = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRecordRow(
    index: Int,
    record: CheckInRecordItem,
    compactLayout: Boolean,
    onShowDetail: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalPulseStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .combinedClickable(onClick = onShowDetail, onLongClick = onDelete)
            .padding(horizontal = if (compactLayout) 14.dp else 16.dp, vertical = if (compactLayout) 12.dp else 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(strings.nthCheckIn(index), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (record.isBackfilled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = strings.backfilledRecord,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (!record.note.isNullOrBlank()) {
                RecordNoteBadge(text = strings.noteLabel)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(record.displayTime.format(recordTimeFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}








