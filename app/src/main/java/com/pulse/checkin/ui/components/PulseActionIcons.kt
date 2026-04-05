package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

enum class PulseIconKind {
    Add,
    Edit,
    Check,
    Records,
    Collapse,
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
    Canvas(modifier = modifier.size(if (compactLayout) 18.dp else 20.dp).padding(1.dp)) {
        val stroke = size.minDimension * 0.12f
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
        }
    }
}
