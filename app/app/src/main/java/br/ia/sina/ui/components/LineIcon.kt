package br.ia.sina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"
private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) = "M${cx - rx} ${cy}a$rx $ry 0 1 0 ${2 * rx} 0a$rx $ry 0 1 0 ${-2 * rx} 0"
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

enum class LineIcons(vararg val paths: String) {
    Back("M20 12H4", "M10 6l-6 6 6 6"),
    Close("M6 6l12 12", "M18 6L6 18"),
    ArrowRight("M4 12h16", "M14 6l6 6-6 6"),
    Chevron("M9 6l6 6-6 6"),
    Check("M20 6L9 17l-5-5"),
    Plus("M12 5v14", "M5 12h14"),
    Warning("M12 3L2 20h20L12 3z", "M12 10v4", "M12 17v.5"),
    Alert(circle(12f, 12f, 9f), "M12 8v5", "M12 16.5v.5"),
    Clock(circle(12f, 12f, 9f), "M12 7v5l3 2"),
    Bell("M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8", "M10 20a2 2 0 0 0 4 0"),
    BellOff("M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8", "M3 3l18 18"),
    Shield("M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6l8-3z"),
    Gear(circle(12f, 12f, 3f), "M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"),
    Lock(rect(4f, 10f, 16f, 11f, 2f), "M8 10V7a4 4 0 0 1 8 0v3"),
    Sparkle("M12 2l2.4 5.6L20 10l-5.6 2.4L12 18l-2.4-5.6L4 10l5.6-2.4z"),
    NoDatabase(ellipse(12f, 6f, 8f, 3f), "M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6", "M3 3l18 18"),
    Battery(rect(2f, 7f, 18f, 10f, 2f), "M22 11v2", "M6 10v4"),
    NoWifi("M2 8.5a15 15 0 0 1 20 0", "M5 12a10 10 0 0 1 14 0", "M3 3l18 18"),
    Phone("M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.5c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z"),
    PersonAdd(circle(9f, 8f, 3.5f), "M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6", "M19 8v6M16 11h6"),
}

@Composable
fun LineIcon(
    icon: LineIcons,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 2f,
    accent: Pair<String, Color>? = null,
) {
    val paths = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    val accentPath = remember(accent?.first) { accent?.let { PathParser().parsePathString(it.first).toPath() } }
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, pivot = Offset.Zero) {
            val stroke = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { drawPath(it, color, style = stroke) }
            if (accentPath != null && accent != null) drawPath(accentPath, accent.second, style = stroke)
        }
    }
}
