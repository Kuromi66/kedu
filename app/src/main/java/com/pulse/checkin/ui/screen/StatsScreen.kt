package com.pulse.checkin.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.checkin.domain.stats.HourlyDistributionBucket
import com.pulse.checkin.domain.stats.MonthlyDetailRow
import com.pulse.checkin.domain.stats.MonthlyTrendPoint
import com.pulse.checkin.domain.stats.YearHabitOption
import com.pulse.checkin.domain.stats.YearSnapshot
import com.pulse.checkin.ui.components.GlassCard
import com.pulse.checkin.ui.components.HabitGlyph
import com.pulse.checkin.ui.components.HeaderFilterButton
import com.pulse.checkin.ui.components.PulseActionIcon
import com.pulse.checkin.ui.components.PulseIconButton
import com.pulse.checkin.ui.components.PulseIconKind
import com.pulse.checkin.ui.components.ScreenHeader
import com.pulse.checkin.ui.components.ShareImageDialog
import com.pulse.checkin.ui.components.StatsShareCard
import com.pulse.checkin.ui.i18n.LocalPulseStrings
import com.pulse.checkin.ui.util.saveBitmapToGallery
import com.pulse.checkin.ui.util.shareBitmap
import com.pulse.checkin.ui.util.toPulseColor
import java.time.YearMonth
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun StatsScreen(
    snapshot: YearSnapshot,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onBackToCurrentYear: () -> Unit,
    onSelectHabit: (String) -> Unit,
) {
    val compactLayout = LocalConfiguration.current.screenWidthDp <= 360
    val strings = LocalPulseStrings.current
    val context = LocalContext.current
    val horizontalPadding = if (compactLayout) 16.dp else 20.dp
    val contentSpacing = if (compactLayout) 12.dp else 16.dp
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var shareBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val shareLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val currentYear = YearMonth.now().year
    val canGoToNextYear = snapshot.year < currentYear

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = strings.statsTitle,
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
                if (snapshot.habitOptions.isEmpty()) {
                    item {
                        GlassCard {
                            Text(strings.noHabitDataTitle, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.noHabitDataDesc,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    item(key = "stats-filters") {
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
                                    items(snapshot.habitOptions, key = { it.habit.id }) { option ->
                                        StatsHabitChip(
                                            option = option,
                                            selected = option.habit.id == snapshot.selectedHabitId,
                                            onClick = { onSelectHabit(option.habit.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "stats-year-switcher") {
                        YearSwitcherCard(
                            year = snapshot.year,
                            compactLayout = compactLayout,
                            onPreviousYear = onPreviousYear,
                            onNextYear = onNextYear,
                            canGoToNextYear = canGoToNextYear,
                            onBackToCurrentYear = onBackToCurrentYear,
                        )
                    }
                    item(key = "stats-summary-metrics") {
                        StatsOverviewCard(snapshot = snapshot, compactLayout = compactLayout)
                    }
                    item(key = "stats-trend-chart") {
                        TrendChartCard(snapshot.trendPoints, compactLayout)
                    }
                    item(key = "stats-hourly-chart") {
                        HourlyDistributionCard(snapshot.hourlyDistribution, compactLayout)
                    }
                    item(key = "stats-monthly-detail") {
                        MonthlyDetailCard(snapshot.monthlyDetails, compactLayout)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(340.dp, 500.dp)
                .drawWithContent {
                    shareLayer.record { this@drawWithContent.drawContent() }
                },
        ) {
            StatsShareCard(snapshot = snapshot, strings = strings)
        }
    }

    if (showShareOptions) {
        ShareImageDialog(
            title = strings.shareStatsTitle,
            description = strings.shareStatsDesc,
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
}

@Composable
private fun YearSwitcherCard(
    year: Int,
    compactLayout: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    canGoToNextYear: Boolean,
    onBackToCurrentYear: () -> Unit,
) {
    val strings = LocalPulseStrings.current
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            YearSwitchButton(type = YearSwitchType.Previous, compactLayout = compactLayout, onClick = onPreviousYear)
            Text(
                text = strings.yearText(year),
                style = if (compactLayout) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                YearSwitchButton(
                    type = YearSwitchType.Current,
                    compactLayout = compactLayout,
                    enabled = year != YearMonth.now().year,
                    onClick = onBackToCurrentYear,
                )
                YearSwitchButton(
                    type = YearSwitchType.Next,
                    compactLayout = compactLayout,
                    enabled = canGoToNextYear,
                    onClick = onNextYear,
                )
            }
        }
    }
}

@Composable
private fun StatsHabitChip(
    option: YearHabitOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalPulseStrings.current
    val accentColor = option.habit.colorArgb.toPulseColor()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White.copy(alpha = 0.24f) else accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    HabitGlyph(
                        glyph = option.habit.glyph,
                        color = if (selected) Color.White else accentColor,
                        modifier = Modifier.size(12.dp),
                        compactLayout = true,
                        textStyle = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(option.habit.name, fontWeight = FontWeight.Medium)
                    Text(
                        text = strings.countTimes(option.yearCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
private fun StatsOverviewCard(snapshot: YearSnapshot, compactLayout: Boolean) {
    val strings = LocalPulseStrings.current
    val completionProgress = if (snapshot.summaryMetrics.trackedDayCount == 0) {
        0f
    } else {
        snapshot.summaryMetrics.completionDayCount.toFloat() / snapshot.summaryMetrics.trackedDayCount.toFloat()
    }.coerceIn(0f, 1f)
    val completionPercent = (completionProgress * 100f).toInt()
    val animatedCompletionProgress by animateFloatAsState(
        targetValue = completionProgress,
        animationSpec = tween(durationMillis = 420),
        label = "stats-completion-progress",
    )
    val animatedCompletionPercent by animateIntAsState(
        targetValue = completionPercent,
        animationSpec = tween(durationMillis = 320),
        label = "stats-completion-percent",
    )
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
                    if (animatedCompletionProgress > 0f) {
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
                StatsOverviewMetric(
                    value = snapshot.summaryMetrics.activeDayCount.toString(),
                    label = strings.activeDaysMetric,
                    valueStyle = metricValueStyle,
                    labelStyle = metricLabelStyle,
                    modifier = Modifier.weight(1f),
                    labelTopPadding = metricLabelTopPadding,
                )
                StatsOverviewMetric(
                    value = snapshot.summaryMetrics.totalCount.toString(),
                    label = strings.totalCheckInsMetric,
                    valueStyle = metricValueStyle,
                    labelStyle = metricLabelStyle,
                    modifier = Modifier.weight(1f),
                    labelTopPadding = metricLabelTopPadding,
                )
                StatsOverviewMetric(
                    value = snapshot.summaryMetrics.longestStreak.toString(),
                    label = strings.longestStreakMetric,
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
private fun StatsOverviewMetric(
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
            modifier = Modifier.padding(top = labelTopPadding),
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun ChartHeader(
    title: String,
    value: String?,
    compactLayout: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        value?.let {
            Text(
                text = it,
                style = if (compactLayout) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun TrendChartCard(points: List<MonthlyTrendPoint>, compactLayout: Boolean) {
    val strings = LocalPulseStrings.current
    var previewIndex by rememberSaveable(points.hashCode(), points.firstOrNull()?.month?.year) { mutableStateOf<Int?>(null) }
    val previewPoint = previewIndex?.let { index -> points.getOrNull(index) }
    val previewText = previewPoint?.let { point ->
        strings.trendPreview(point.month, point.totalCount)
    }

    GlassCard {
        ChartHeader(
            title = strings.trendTitle,
            value = previewText,
            compactLayout = compactLayout,
        )
        Spacer(modifier = Modifier.height(10.dp))
        TrendLineChart(
            points = points,
            compactLayout = compactLayout,
            previewIndex = previewIndex,
            onPreviewIndexChange = { previewIndex = it },
        )
    }
}

@Composable
private fun TrendLineChart(
    points: List<MonthlyTrendPoint>,
    compactLayout: Boolean,
    previewIndex: Int?,
    onPreviewIndexChange: (Int?) -> Unit,
) {
    val strings = LocalPulseStrings.current
    val targetMaxValue = (points.maxOfOrNull { it.totalCount } ?: 0).coerceAtLeast(1)
    val animatedRatios = points.mapIndexed { index, point ->
        animateFloatAsState(
            targetValue = point.totalCount.toFloat() / targetMaxValue.toFloat(),
            animationSpec = tween(durationMillis = 480),
            label = "trend-point-$index",
        ).value
    }
    val chartHeight = if (compactLayout) 168.dp else 184.dp
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointFillColor = MaterialTheme.colorScheme.surface

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 18.dp.toPx()
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeightPx = size.height - topPadding - bottomPadding
                val stepX = if (points.size <= 1) 0f else chartWidth / (points.size - 1)

                repeat(4) { index ->
                    val y = topPadding + chartHeightPx * index / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val chartPoints = points.mapIndexed { index, _ ->
                    val ratio = animatedRatios[index]
                    val x = leftPadding + stepX * index
                    val y = topPadding + chartHeightPx * (1f - ratio)
                    Offset(x, y)
                }
                if (chartPoints.isNotEmpty()) {
                    val smoothPath = Path().apply {
                        moveTo(chartPoints.first().x, chartPoints.first().y)
                        for (index in 1 until chartPoints.size) {
                            val previous = chartPoints[index - 1]
                            val current = chartPoints[index]
                            val controlX = previous.x + (current.x - previous.x) / 2f
                            cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
                        }
                    }
                    val baselineY = topPadding + chartHeightPx
                    val areaPath = Path().apply {
                        addPath(smoothPath)
                        lineTo(chartPoints.last().x, baselineY)
                        lineTo(chartPoints.first().x, baselineY)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0.03f)),
                            startY = topPadding,
                            endY = baselineY,
                        ),
                    )
                    drawPath(
                        path = smoothPath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    previewIndex?.takeIf { it in chartPoints.indices }?.let { activeIndex ->
                        val selectedPoint = chartPoints[activeIndex]
                        drawLine(
                            color = lineColor.copy(alpha = 0.18f),
                            start = Offset(selectedPoint.x, topPadding),
                            end = Offset(selectedPoint.x, baselineY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    chartPoints.forEachIndexed { index, point ->
                        val selected = index == previewIndex
                        drawCircle(color = pointFillColor, radius = if (selected) 7.dp.toPx() else 5.dp.toPx(), center = point)
                        drawCircle(color = lineColor, radius = if (selected) 4.dp.toPx() else 3.dp.toPx(), center = point)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points.hashCode(), points.firstOrNull()?.month?.year) {
                        if (points.isEmpty()) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val widthPerItem = size.width.toFloat() / points.size.toFloat()
                                val index = (offset.x / widthPerItem).toInt().coerceIn(0, points.lastIndex)
                                onPreviewIndexChange(index)
                            },
                            onHorizontalDrag = { change, _ ->
                                val widthPerItem = size.width.toFloat() / points.size.toFloat()
                                val index = (change.position.x / widthPerItem).toInt().coerceIn(0, points.lastIndex)
                                onPreviewIndexChange(index)
                            },
                            onDragEnd = { onPreviewIndexChange(null) },
                            onDragCancel = { onPreviewIndexChange(null) },
                        )
                    },
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEachIndexed { index, point ->
                if (index in listOf(0, 2, 4, 6, 8, 10, 11)) {
                    Text(
                        text = strings.statsMonthText(point.month),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == previewIndex) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == previewIndex) FontWeight.SemiBold else FontWeight.Normal,
                    )
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }
            }
        }
    }
}

@Composable
private fun HourlyDistributionCard(buckets: List<HourlyDistributionBucket>, compactLayout: Boolean) {
    val strings = LocalPulseStrings.current
    var previewHour by rememberSaveable(buckets.hashCode(), buckets.sumOf { it.count }) { mutableStateOf<Int?>(null) }
    val previewBucket = previewHour?.let { hour -> buckets.firstOrNull { it.hour == hour } }
    val previewText = previewBucket?.let { bucket ->
        strings.hourlyPreview(bucket.hour, bucket.count)
    }

    GlassCard {
        ChartHeader(
            title = strings.hourlyDistributionTitle,
            value = previewText,
            compactLayout = compactLayout,
        )
        Spacer(modifier = Modifier.height(10.dp))
        HourlyBarChart(
            buckets = buckets,
            compactLayout = compactLayout,
            previewHour = previewHour,
            onPreviewHourChange = { previewHour = it },
        )
    }
}

@Composable
private fun HourlyBarChart(
    buckets: List<HourlyDistributionBucket>,
    compactLayout: Boolean,
    previewHour: Int?,
    onPreviewHourChange: (Int?) -> Unit,
) {
    val targetMaxValue = (buckets.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val animatedRatios = buckets.mapIndexed { index, bucket ->
        animateFloatAsState(
            targetValue = bucket.count.toFloat() / targetMaxValue.toFloat(),
            animationSpec = tween(durationMillis = 480),
            label = "hourly-bar-$index",
        ).value
    }
    val barColor = MaterialTheme.colorScheme.primary
    val mutedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val chartHeight = if (compactLayout) 132.dp else 146.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(buckets.hashCode(), buckets.sumOf { it.count }) {
                if (buckets.isEmpty()) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val widthPerItem = size.width.toFloat() / buckets.size.toFloat()
                        val index = (offset.x / widthPerItem).toInt().coerceIn(0, buckets.lastIndex)
                        onPreviewHourChange(buckets[index].hour)
                    },
                    onHorizontalDrag = { change, _ ->
                        val widthPerItem = size.width.toFloat() / buckets.size.toFloat()
                        val index = (change.position.x / widthPerItem).toInt().coerceIn(0, buckets.lastIndex)
                        onPreviewHourChange(buckets[index].hour)
                    },
                    onDragEnd = { onPreviewHourChange(null) },
                    onDragCancel = { onPreviewHourChange(null) },
                )
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 3.dp else 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val ratio = animatedRatios[index]
                    val selected = bucket.hour == previewHour
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((chartHeight * ratio).coerceAtLeast(6.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    when {
                                        bucket.count == 0 -> mutedBarColor
                                        selected -> barColor
                                        else -> barColor.copy(alpha = 0.42f)
                                    },
                                ),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 6, 12, 18, 23).forEach { hour ->
                    Text(
                        text = hour.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hour == previewHour) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hour == previewHour) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyDetailCard(rows: List<MonthlyDetailRow>, compactLayout: Boolean) {
    GlassCard {
        Text(LocalPulseStrings.current.monthlyDetailsTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(if (compactLayout) 14.dp else 16.dp))
        DetailTableHeader(compactLayout = compactLayout)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                DetailTableRow(row = row, compactLayout = compactLayout)
            }
        }
    }
}

@Composable
private fun DetailTableHeader(compactLayout: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compactLayout) 12.dp else 14.dp, vertical = if (compactLayout) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val strings = LocalPulseStrings.current
        DetailCell(strings.detailMonth, 1.1f, true, center = false)
        DetailCell(strings.detailCompletion, 0.95f, true, center = true)
        DetailCell(strings.detailDays, 0.9f, true, center = true)
        DetailCell(strings.detailCount, 0.9f, true, center = true)
        DetailCell(strings.detailStreak, 1.05f, true, center = true)
    }
}

@Composable
private fun DetailTableRow(row: MonthlyDetailRow, compactLayout: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
            .padding(horizontal = if (compactLayout) 12.dp else 14.dp, vertical = if (compactLayout) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailCell(LocalPulseStrings.current.statsMonthText(row.month), 1.1f, false, center = false)
        DetailCell("${(row.completionRate * 100f).toInt()}%", 0.95f, false, center = true)
        DetailCell(row.activeDayCount.toString(), 0.9f, false, center = true)
        DetailCell(row.totalCount.toString(), 0.9f, false, center = true)
        DetailCell(if (row.longestStreak == 0) "--" else LocalPulseStrings.current.streakDays(row.longestStreak), 1.05f, false, center = true)
    }
}

@Composable
private fun RowScope.DetailCell(text: String, weight: Float, header: Boolean, center: Boolean) {
    Box(
        modifier = Modifier.weight(weight),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
        )
    }
}

private enum class YearSwitchType {
    Previous,
    Current,
    Next,
}

@Composable
private fun YearSwitchButton(
    type: YearSwitchType,
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
        YearSwitchType.Previous -> PulseIconKind.ArrowLeft
        YearSwitchType.Next -> PulseIconKind.ArrowRight
        YearSwitchType.Current -> PulseIconKind.CurrentTime
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
