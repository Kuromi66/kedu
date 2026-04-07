package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

data class HabitIconPreset(
    val token: String,
    val label: String,
)

internal sealed interface LucideNode
internal data class LucidePathNode(val d: String) : LucideNode
internal data class LucideCircleNode(val cx: Float, val cy: Float, val r: Float) : LucideNode
internal data class LucideLineNode(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : LucideNode
internal data class LucideRectNode(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float = 0f,
    val ry: Float = 0f,
) : LucideNode
internal data class LucidePoint(val x: Float, val y: Float)
internal data class LucidePolylineNode(val points: List<LucidePoint>) : LucideNode

private sealed interface PreparedLucideNode
private data class PreparedPathNode(val path: Path) : PreparedLucideNode
private data class PreparedCircleNode(val cx: Float, val cy: Float, val r: Float) : PreparedLucideNode
private data class PreparedLineNode(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : PreparedLucideNode
private data class PreparedRectNode(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float,
    val ry: Float,
) : PreparedLucideNode
private data class PreparedPolylineNode(val points: List<Offset>) : PreparedLucideNode
private data class PreparedIcon(val nodes: List<PreparedLucideNode>, val bounds: Rect)

@Composable
fun HabitGlyph(
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    compactLayout: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val iconToken = resolveHabitIconToken(glyph)
    if (iconToken == null) {
        val displayGlyph = glyph.take(2)
        val adjustedStyle = if (displayGlyph.length > 1) {
            textStyle.copy(
                fontSize = if (textStyle.fontSize.isSpecified) textStyle.fontSize * 0.7f else MaterialTheme.typography.titleMedium.fontSize,
                letterSpacing = if (textStyle.letterSpacing.isSpecified) textStyle.letterSpacing * 0.2f else androidx.compose.ui.unit.TextUnit.Unspecified,
            )
        } else {
            textStyle
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = displayGlyph,
                color = color,
                style = adjustedStyle,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        return
    }

    val preparedIcon = remember(iconToken) {
        val nodes = habitIconNodesFor(iconToken).orEmpty().map { node ->
            when (node) {
                is LucidePathNode -> PreparedPathNode(PathParser().parsePathString(node.d).toPath())
                is LucideCircleNode -> PreparedCircleNode(node.cx, node.cy, node.r)
                is LucideLineNode -> PreparedLineNode(node.x1, node.y1, node.x2, node.y2)
                is LucideRectNode -> PreparedRectNode(node.x, node.y, node.width, node.height, node.rx, node.ry)
                is LucidePolylineNode -> PreparedPolylineNode(node.points.map { Offset(it.x, it.y) })
            }
        }
        PreparedIcon(nodes = nodes, bounds = calculatePreparedBounds(nodes))
    }

    Canvas(modifier = modifier) {
        val strokeWidth = if (compactLayout) 1.9f else 2f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bounds = preparedIcon.bounds
        val padding = if (compactLayout) 2.2f else 2.6f
        val contentWidth = (bounds.width + padding * 2f).coerceAtLeast(1f)
        val contentHeight = (bounds.height + padding * 2f).coerceAtLeast(1f)
        val scaleFactor = minOf(size.width / contentWidth, size.height / contentHeight)

        withTransform({
            translate(left = size.width / 2f, top = size.height / 2f)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            translate(left = -bounds.center.x, top = -bounds.center.y)
        }) {
            preparedIcon.nodes.forEach { node ->
                when (node) {
                    is PreparedPathNode -> drawPath(node.path, color = color, style = stroke)
                    is PreparedCircleNode -> drawCircle(color = color, radius = node.r, center = Offset(node.cx, node.cy), style = stroke)
                    is PreparedLineNode -> drawLine(color = color, start = Offset(node.x1, node.y1), end = Offset(node.x2, node.y2), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    is PreparedRectNode -> drawRoundRect(
                        color = color,
                        topLeft = Offset(node.x, node.y),
                        size = androidx.compose.ui.geometry.Size(node.width, node.height),
                        cornerRadius = CornerRadius(node.rx, node.ry),
                        style = stroke,
                    )
                    is PreparedPolylineNode -> {
                        val points = node.points
                        if (points.size >= 2) {
                            points.zipWithNext().forEach { (start, end) ->
                                drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePreparedBounds(nodes: List<PreparedLucideNode>): Rect {
    var bounds: Rect? = null
    nodes.forEach { node ->
        val nodeBounds = when (node) {
            is PreparedPathNode -> node.path.getBounds()
            is PreparedCircleNode -> Rect(
                left = node.cx - node.r,
                top = node.cy - node.r,
                right = node.cx + node.r,
                bottom = node.cy + node.r,
            )
            is PreparedLineNode -> Rect(
                left = minOf(node.x1, node.x2),
                top = minOf(node.y1, node.y2),
                right = maxOf(node.x1, node.x2),
                bottom = maxOf(node.y1, node.y2),
            )
            is PreparedRectNode -> Rect(node.x, node.y, node.x + node.width, node.y + node.height)
            is PreparedPolylineNode -> {
                val xs = node.points.map { it.x }
                val ys = node.points.map { it.y }
                Rect(
                    left = xs.minOrNull() ?: 0f,
                    top = ys.minOrNull() ?: 0f,
                    right = xs.maxOrNull() ?: 0f,
                    bottom = ys.maxOrNull() ?: 0f,
                )
            }
        }
        bounds = bounds?.let { current ->
            Rect(
                left = minOf(current.left, nodeBounds.left),
                top = minOf(current.top, nodeBounds.top),
                right = maxOf(current.right, nodeBounds.right),
                bottom = maxOf(current.bottom, nodeBounds.bottom),
            )
        } ?: nodeBounds
    }
    return bounds ?: Rect(Offset.Zero, Size(24f, 24f))
}
