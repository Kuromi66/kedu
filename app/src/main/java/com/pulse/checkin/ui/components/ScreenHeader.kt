package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun ScreenHeader(
    title: String,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(if (compactLayout) 52.dp else 58.dp)
            .padding(horizontal = if (compactLayout) 16.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = if (compactLayout) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (action != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                action()
            }
        }
    }
}

@Composable
fun HeaderFilterButton(
    active: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .size(if (compactLayout) 38.dp else 42.dp)
            .background(color = containerColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(if (compactLayout) 9.dp else 10.dp)) {
            val stroke = size.minDimension * 0.12f
            val y1 = size.height * 0.25f
            val y2 = size.height * 0.5f
            val y3 = size.height * 0.75f
            drawFilterLine(accentColor, y1, stroke, size.width * 0.34f)
            drawFilterLine(accentColor, y2, stroke, size.width * 0.68f)
            drawFilterLine(accentColor, y3, stroke, size.width * 0.46f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFilterLine(
    color: Color,
    y: Float,
    stroke: Float,
    knobX: Float,
) {
    drawLine(
        color = color,
        start = Offset(size.width * 0.12f, y),
        end = Offset(size.width * 0.88f, y),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawCircle(color = color, radius = stroke * 1.2f, center = Offset(knobX, y))
}
