package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PulseIconKind {
    Add,
    Edit,
    Check,
    Records,
    Collapse,
    Filter,
    ArrowLeft,
    ArrowRight,
    CurrentTime,
    Theme,
    Data,
    Bell,
    Info,
    Language,
    Download,
    Upload,
    Account,
    Share,
    Calendar,
    LightMode,
    DarkMode,
    SystemMode,
    TodayTab,
    HistoryTab,
    StatsTab,
    SettingsTab,
}

@Composable
fun PulseIconButton(
    kind: PulseIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compactLayout: Boolean = false,
    highlighted: Boolean = false,
) {
    val size = if (compactLayout) 34.dp else 38.dp
    val iconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = when {
        highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PulseActionIcon(kind = kind, color = iconColor, compactLayout = compactLayout)
    }
}

@Composable
fun PulsePrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactLayout: Boolean = false,
    label: String? = null,
    icon: PulseIconKind? = null,
) {
    val width = if (compactLayout) 64.dp else 72.dp
    val height = if (compactLayout) 40.dp else 44.dp

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            label != null -> Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
            icon != null -> PulseActionIcon(icon, color = MaterialTheme.colorScheme.onPrimary, compactLayout = compactLayout)
        }
    }
}

@Composable
fun PulseActionIcon(
    kind: PulseIconKind,
    color: Color,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    if (kind in pulseLibraryIconKinds) {
        PulseLibraryIcon(kind = kind, color = color, compactLayout = compactLayout, modifier = modifier)
        return
    }

    Canvas(modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp).then(modifier)) {
        val stroke = when (kind) {
            PulseIconKind.TodayTab,
            PulseIconKind.HistoryTab,
            PulseIconKind.StatsTab,
            PulseIconKind.SettingsTab -> size.minDimension * 0.14f
            PulseIconKind.Filter -> size.minDimension * 0.12f
            else -> size.minDimension * 0.12f
        }
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        when (kind) {
            PulseIconKind.Add -> {
                drawLine(color, Offset(centerX, size.height * 0.18f), Offset(centerX, size.height * 0.82f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.18f, centerY), Offset(size.width * 0.82f, centerY), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Edit -> {
                drawLine(color, Offset(size.width * 0.30f, size.height * 0.72f), Offset(size.width * 0.64f, size.height * 0.38f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.40f, size.height * 0.82f), Offset(size.width * 0.74f, size.height * 0.48f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.64f, size.height * 0.38f), Offset(size.width * 0.74f, size.height * 0.48f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.26f, size.height * 0.86f), Offset(size.width * 0.30f, size.height * 0.72f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.26f, size.height * 0.86f), Offset(size.width * 0.40f, size.height * 0.82f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.70f, size.height * 0.32f), Offset(size.width * 0.82f, size.height * 0.44f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Check -> {
                drawLine(color, Offset(size.width * 0.24f, centerY), Offset(size.width * 0.44f, size.height * 0.70f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.44f, size.height * 0.70f), Offset(size.width * 0.78f, size.height * 0.30f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Records -> {
                listOf(0.26f, 0.50f, 0.74f).forEach { y ->
                    drawLine(color, Offset(size.width * 0.26f, size.height * y), Offset(size.width * 0.78f, size.height * y), stroke, cap = StrokeCap.Round)
                    drawCircle(color, radius = stroke * 0.65f, center = Offset(size.width * 0.14f, size.height * y))
                }
            }
            PulseIconKind.Collapse -> {
                drawLine(color, Offset(size.width * 0.24f, size.height * 0.62f), Offset(centerX, size.height * 0.38f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(centerX, size.height * 0.38f), Offset(size.width * 0.76f, size.height * 0.62f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Filter -> {
                val ys = listOf(size.height * 0.24f, centerY, size.height * 0.76f)
                val knobs = listOf(size.width * 0.68f, size.width * 0.38f, size.width * 0.58f)
                ys.zip(knobs).forEach { (y, knobX) ->
                    drawLine(color, Offset(size.width * 0.18f, y), Offset(size.width * 0.82f, y), stroke, cap = StrokeCap.Round)
                    drawCircle(color, radius = stroke * 0.7f, center = Offset(knobX, y))
                }
            }
            PulseIconKind.ArrowLeft -> {
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.22f), Offset(size.width * 0.36f, centerY), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.36f, centerY), Offset(size.width * 0.62f, size.height * 0.78f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.ArrowRight -> {
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.22f), Offset(size.width * 0.64f, centerY), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.64f, centerY), Offset(size.width * 0.38f, size.height * 0.78f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Account -> {
                drawCircle(
                    color,
                    radius = size.minDimension * 0.17f,
                    center = Offset(centerX, size.height * 0.32f),
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.20f, size.height * 0.46f),
                    size = Size(size.width * 0.60f, size.height * 0.44f),
                    style = Stroke(width = stroke),
                )
            }
            PulseIconKind.Share -> {
                drawCircle(color, radius = stroke * 0.9f, center = Offset(size.width * 0.24f, size.height * 0.30f))
                drawCircle(color, radius = stroke * 0.9f, center = Offset(size.width * 0.76f, size.height * 0.28f))
                drawCircle(color, radius = stroke * 0.9f, center = Offset(size.width * 0.52f, size.height * 0.72f))
                drawLine(color, Offset(size.width * 0.30f, size.height * 0.36f), Offset(size.width * 0.70f, size.height * 0.32f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.30f, size.height * 0.36f), Offset(size.width * 0.48f, size.height * 0.66f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.70f, size.height * 0.32f), Offset(size.width * 0.56f, size.height * 0.66f), stroke, cap = StrokeCap.Round)
            }
            PulseIconKind.Calendar -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
                    size = Size(size.width * 0.76f, size.height * 0.74f),
                    cornerRadius = CornerRadius(size.width * 0.12f, size.width * 0.12f),
                    style = Stroke(width = stroke),
                )
                drawLine(color, Offset(size.width * 0.12f, size.height * 0.34f), Offset(size.width * 0.88f, size.height * 0.34f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.10f), Offset(size.width * 0.28f, size.height * 0.24f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.10f), Offset(size.width * 0.72f, size.height * 0.24f), stroke, cap = StrokeCap.Round)
                drawCircle(color, radius = stroke * 0.8f, center = Offset(size.width * 0.34f, size.height * 0.56f))
                drawCircle(color, radius = stroke * 0.8f, center = Offset(size.width * 0.62f, size.height * 0.56f))
            }
            PulseIconKind.CurrentTime -> {
                drawCircle(color, radius = size.minDimension * 0.28f, center = Offset(centerX, centerY), style = Stroke(width = stroke))
                drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(centerX, centerY))
            }
            PulseIconKind.TodayTab -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.14f),
                    size = Size(size.width * 0.76f, size.height * 0.74f),
                    cornerRadius = CornerRadius(size.width * 0.17f, size.width * 0.17f),
                    style = Stroke(width = stroke),
                )
                drawLine(color, Offset(size.width * 0.12f, size.height * 0.34f), Offset(size.width * 0.88f, size.height * 0.34f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.10f), Offset(size.width * 0.28f, size.height * 0.22f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.10f), Offset(size.width * 0.72f, size.height * 0.22f), stroke, cap = StrokeCap.Round)
                drawCircle(color, radius = stroke * 0.9f, center = Offset(centerX, size.height * 0.60f))
            }
            PulseIconKind.HistoryTab -> {
                drawCircle(color, radius = size.minDimension * 0.35f, center = Offset(centerX, centerY), style = Stroke(width = stroke))
                drawLine(color, Offset(centerX, centerY), Offset(centerX, size.height * 0.28f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(centerX, centerY), Offset(size.width * 0.70f, size.height * 0.55f), stroke, cap = StrokeCap.Round)
                drawCircle(color, radius = stroke * 0.65f, center = Offset(centerX, centerY))
            }
            PulseIconKind.StatsTab -> {
                val barWidth = size.width * 0.14f
                val bottom = size.height * 0.82f
                val corners = CornerRadius(barWidth, barWidth)
                drawRoundRect(color, topLeft = Offset(size.width * 0.18f, size.height * 0.46f), size = Size(barWidth, bottom - size.height * 0.46f), cornerRadius = corners)
                drawRoundRect(color, topLeft = Offset(size.width * 0.43f, size.height * 0.28f), size = Size(barWidth, bottom - size.height * 0.28f), cornerRadius = corners)
                drawRoundRect(color, topLeft = Offset(size.width * 0.68f, size.height * 0.16f), size = Size(barWidth, bottom - size.height * 0.16f), cornerRadius = corners)
            }
            PulseIconKind.SettingsTab -> {
                drawCircle(color, radius = size.minDimension * 0.18f, center = Offset(centerX, centerY), style = Stroke(width = stroke))
                repeat(8) { index ->
                    val angle = (PI / 4.0 * index) - PI / 2.0
                    val innerRadius = size.minDimension * 0.29f
                    val outerRadius = size.minDimension * 0.43f
                    val start = Offset(
                        x = centerX + (cos(angle) * innerRadius).toFloat(),
                        y = centerY + (sin(angle) * innerRadius).toFloat(),
                    )
                    val end = Offset(
                        x = centerX + (cos(angle) * outerRadius).toFloat(),
                        y = centerY + (sin(angle) * outerRadius).toFloat(),
                    )
                    drawLine(color, start, end, stroke, cap = StrokeCap.Round)
                }
            }
            else -> Unit
        }
    }
}
