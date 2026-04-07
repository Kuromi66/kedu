package com.pulse.checkin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp

internal val pulseLibraryIconKinds = setOf(
    PulseIconKind.Add,
    PulseIconKind.Edit,
    PulseIconKind.Records,
    PulseIconKind.Collapse,
    PulseIconKind.Filter,
    PulseIconKind.ArrowLeft,
    PulseIconKind.ArrowRight,
    PulseIconKind.CurrentTime,
    PulseIconKind.Theme,
    PulseIconKind.Data,
    PulseIconKind.Bell,
    PulseIconKind.Info,
    PulseIconKind.Language,
    PulseIconKind.Download,
    PulseIconKind.Upload,
    PulseIconKind.LightMode,
    PulseIconKind.DarkMode,
    PulseIconKind.SystemMode,
    PulseIconKind.TodayTab,
    PulseIconKind.HistoryTab,
    PulseIconKind.StatsTab,
    PulseIconKind.SettingsTab,
)

private val pulseLibraryIconNodes = mapOf(
    PulseIconKind.Add to listOf(
        LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
        LucidePathNode("M8 12h8"),
        LucidePathNode("M12 8v8"),
    ),
    PulseIconKind.Edit to listOf(
        LucidePathNode("M12 20h9"),
        LucidePathNode("M16.376 3.622a1 1 0 0 1 3.002 3.002L7.368 18.635a2 2 0 0 1-.855.506l-2.872.838a.5.5 0 0 1-.62-.62l.838-2.872a2 2 0 0 1 .506-.854z"),
        LucidePathNode("m15 5 3 3"),
    ),
    PulseIconKind.Records to listOf(
        LucidePathNode("M3 12h.01"),
        LucidePathNode("M3 18h.01"),
        LucidePathNode("M3 6h.01"),
        LucidePathNode("M8 12h13"),
        LucidePathNode("M8 18h13"),
        LucidePathNode("M8 6h13"),
    ),
    PulseIconKind.Collapse to listOf(
        LucidePathNode("m18 15-6-6-6 6"),
    ),
    PulseIconKind.Filter to listOf(
        LucideLineNode(x1 = 21f, y1 = 4f, x2 = 14f, y2 = 4f),
        LucideLineNode(x1 = 10f, y1 = 4f, x2 = 3f, y2 = 4f),
        LucideLineNode(x1 = 21f, y1 = 12f, x2 = 12f, y2 = 12f),
        LucideLineNode(x1 = 8f, y1 = 12f, x2 = 3f, y2 = 12f),
        LucideLineNode(x1 = 21f, y1 = 20f, x2 = 16f, y2 = 20f),
        LucideLineNode(x1 = 12f, y1 = 20f, x2 = 3f, y2 = 20f),
        LucideLineNode(x1 = 14f, y1 = 2f, x2 = 14f, y2 = 6f),
        LucideLineNode(x1 = 8f, y1 = 10f, x2 = 8f, y2 = 14f),
        LucideLineNode(x1 = 16f, y1 = 18f, x2 = 16f, y2 = 22f),
    ),
    PulseIconKind.ArrowLeft to listOf(
        LucidePathNode("m15 18-6-6 6-6"),
    ),
    PulseIconKind.ArrowRight to listOf(
        LucidePathNode("m9 18 6-6-6-6"),
    ),
    PulseIconKind.CurrentTime to listOf(
        LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
        LucidePolylineNode(points = listOf(LucidePoint(12f, 6f), LucidePoint(12f, 12f), LucidePoint(16.5f, 12f))),
    ),
    PulseIconKind.Data to listOf(
        LucidePathNode("M21 5c0 1.657-4.03 3-9 3S3 6.657 3 5s4.03-3 9-3 9 1.343 9 3Z"),
        LucidePathNode("M3 5V19A9 3 0 0 0 21 19V5"),
        LucidePathNode("M3 12c0 1.657 4.03 3 9 3s9-1.343 9-3"),
    ),
    PulseIconKind.Theme to listOf(
        LucidePathNode("M11 17a4 4 0 0 1-8 0V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2Z"),
        LucidePathNode("M16.7 13H19a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H7"),
        LucidePathNode("M7 17h.01"),
        LucidePathNode("m11 8 2.3-2.3a2.4 2.4 0 0 1 3.404.004L18.6 7.6a2.4 2.4 0 0 1 .026 3.434L9.9 19.8"),
    ),
    PulseIconKind.Bell to listOf(
        LucidePathNode("M10.268 21a2 2 0 0 0 3.464 0"),
        LucidePathNode("M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"),
    ),
    PulseIconKind.Info to listOf(
        LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
        LucidePathNode("M12 16v-4"),
        LucidePathNode("M12 8h.01"),
    ),
    PulseIconKind.Language to listOf(
        LucidePathNode("m5 8 6 6"),
        LucidePathNode("m4 14 6-6 2-3"),
        LucidePathNode("M2 5h12"),
        LucidePathNode("M7 2h1"),
        LucidePathNode("m22 22-5-10-5 10"),
        LucidePathNode("M14 18h6"),
    ),
    PulseIconKind.Download to listOf(
        LucidePathNode("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"),
        LucidePolylineNode(points = listOf(LucidePoint(7f, 10f), LucidePoint(12f, 15f), LucidePoint(17f, 10f))),
        LucideLineNode(x1 = 12f, y1 = 15f, x2 = 12f, y2 = 3f),
    ),
    PulseIconKind.Upload to listOf(
        LucidePathNode("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"),
        LucidePolylineNode(points = listOf(LucidePoint(17f, 8f), LucidePoint(12f, 3f), LucidePoint(7f, 8f))),
        LucideLineNode(x1 = 12f, y1 = 3f, x2 = 12f, y2 = 15f),
    ),
    PulseIconKind.LightMode to listOf(
        LucideCircleNode(cx = 12f, cy = 12f, r = 4f),
        LucidePathNode("M12 4h.01"),
        LucidePathNode("M20 12h.01"),
        LucidePathNode("M12 20h.01"),
        LucidePathNode("M4 12h.01"),
        LucidePathNode("M17.657 6.343h.01"),
        LucidePathNode("M17.657 17.657h.01"),
        LucidePathNode("M6.343 17.657h.01"),
        LucidePathNode("M6.343 6.343h.01"),
    ),
    PulseIconKind.DarkMode to listOf(
        LucidePathNode("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"),
    ),
    PulseIconKind.SystemMode to listOf(
        LucidePathNode("M12 17v4"),
        LucidePathNode("m15.2 4.9-.9-.4"),
        LucidePathNode("m15.2 7.1-.9.4"),
        LucidePathNode("m16.9 3.2-.4-.9"),
        LucidePathNode("m16.9 8.8-.4.9"),
        LucidePathNode("m19.5 2.3-.4.9"),
        LucidePathNode("m19.5 9.7-.4-.9"),
        LucidePathNode("m21.7 4.5-.9.4"),
        LucidePathNode("m21.7 7.5-.9-.4"),
        LucidePathNode("M22 13v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7"),
        LucidePathNode("M8 21h8"),
        LucideCircleNode(cx = 18f, cy = 6f, r = 3f),
    ),
    PulseIconKind.TodayTab to listOf(
        LucidePathNode("M8 2v4"),
        LucidePathNode("M16 2v4"),
        LucideRectNode(x = 3f, y = 4f, width = 18f, height = 18f, rx = 2f, ry = 2f),
        LucidePathNode("M3 10h18"),
    ),
    PulseIconKind.HistoryTab to listOf(
        LucideCircleNode(cx = 12f, cy = 12f, r = 10f),
        LucidePolylineNode(points = listOf(LucidePoint(12f, 6f), LucidePoint(12f, 12f), LucidePoint(16f, 14f))),
    ),
    PulseIconKind.StatsTab to listOf(
        LucidePathNode("M3 3v16a2 2 0 0 0 2 2h16"),
        LucidePathNode("M18 17V9"),
        LucidePathNode("M13 17V5"),
        LucidePathNode("M8 17v-3"),
    ),
    PulseIconKind.SettingsTab to listOf(
        LucidePathNode("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"),
        LucideCircleNode(cx = 12f, cy = 12f, r = 3f),
    ),
)

private sealed interface PreparedPulseNode
private data class PreparedPulsePath(val path: Path) : PreparedPulseNode
private data class PreparedPulseCircle(val cx: Float, val cy: Float, val r: Float) : PreparedPulseNode
private data class PreparedPulseRect(val x: Float, val y: Float, val width: Float, val height: Float, val rx: Float, val ry: Float) : PreparedPulseNode
private data class PreparedPulsePolyline(val points: List<Offset>) : PreparedPulseNode
private data class PreparedPulseIcon(val nodes: List<PreparedPulseNode>, val bounds: Rect)

@Composable
internal fun PulseLibraryIcon(
    kind: PulseIconKind,
    color: Color,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    val nodes = pulseLibraryIconNodes[kind] ?: return
    val prepared = remember(kind) {
        val preparedNodes = nodes.map { node ->
            when (node) {
                is LucidePathNode -> PreparedPulsePath(PathParser().parsePathString(node.d).toPath())
                is LucideCircleNode -> PreparedPulseCircle(node.cx, node.cy, node.r)
                is LucideRectNode -> PreparedPulseRect(node.x, node.y, node.width, node.height, node.rx, node.ry)
                is LucidePolylineNode -> PreparedPulsePolyline(node.points.map { Offset(it.x, it.y) })
                is LucideLineNode -> PreparedPulsePolyline(listOf(Offset(node.x1, node.y1), Offset(node.x2, node.y2)))
            }
        }
        PreparedPulseIcon(preparedNodes, calculatePulseBounds(preparedNodes))
    }

    Canvas(modifier = Modifier.size(if (compactLayout) 18.dp else 20.dp).then(modifier)) {
        val strokeWidth = if (compactLayout) 1.9f else 2f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bounds = prepared.bounds
        val padding = if (compactLayout) 2.2f else 2.6f
        val contentWidth = (bounds.width + padding * 2f).coerceAtLeast(1f)
        val contentHeight = (bounds.height + padding * 2f).coerceAtLeast(1f)
        val scaleFactor = minOf(size.width / contentWidth, size.height / contentHeight)

        withTransform({
            translate(left = size.width / 2f, top = size.height / 2f)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            translate(left = -bounds.center.x, top = -bounds.center.y)
        }) {
            prepared.nodes.forEach { node ->
                when (node) {
                    is PreparedPulsePath -> drawPath(node.path, color = color, style = stroke)
                    is PreparedPulseCircle -> drawCircle(color = color, radius = node.r, center = Offset(node.cx, node.cy), style = stroke)
                    is PreparedPulseRect -> drawRoundRect(
                        color = color,
                        topLeft = Offset(node.x, node.y),
                        size = Size(node.width, node.height),
                        cornerRadius = CornerRadius(node.rx, node.ry),
                        style = stroke,
                    )
                    is PreparedPulsePolyline -> {
                        if (node.points.size >= 2) {
                            node.points.zipWithNext().forEach { (start, end) ->
                                drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePulseBounds(nodes: List<PreparedPulseNode>): Rect {
    var bounds: Rect? = null
    nodes.forEach { node ->
        val nodeBounds = when (node) {
            is PreparedPulsePath -> node.path.getBounds()
            is PreparedPulseCircle -> Rect(node.cx - node.r, node.cy - node.r, node.cx + node.r, node.cy + node.r)
            is PreparedPulseRect -> Rect(node.x, node.y, node.x + node.width, node.y + node.height)
            is PreparedPulsePolyline -> {
                val xs = node.points.map { it.x }
                val ys = node.points.map { it.y }
                Rect(xs.minOrNull() ?: 0f, ys.minOrNull() ?: 0f, xs.maxOrNull() ?: 0f, ys.maxOrNull() ?: 0f)
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
