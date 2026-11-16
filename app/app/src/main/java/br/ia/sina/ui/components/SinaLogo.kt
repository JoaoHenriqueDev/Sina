package br.ia.sina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import br.ia.sina.ui.theme.SinaColors

private const val S_PATH =
    "M45.5 20C45.5 13 38.9 9.5 32 9.5C25.1 9.5 18.5 13.5 18.5 20C18.5 27.5 25.1 30 32 32.5" +
        "C38.9 35 45.5 37.5 45.5 45C45.5 51.5 38.9 55.5 32 55.5C25.1 55.5 18.5 52 18.5 45"

@Composable
fun SinaLogo(size: Dp, modifier: Modifier = Modifier, withBackground: Boolean = true) {
    val sPath = remember { PathParser().parsePathString(S_PATH).toPath() }
    val strokeWidth = if (size < 40.dp) 5f else 4.2f
    Canvas(
        modifier
            .size(size)
            .then(if (withBackground) Modifier.clip(RoundedCornerShape(size * 0.28f)).background(SinaColors.Dark) else Modifier)
            .semantics { contentDescription = "Logo do Sina" },
    ) {
        val k = this.size.minDimension / 64f
        scale(k, pivot = Offset.Zero) {
            drawRoundRect(SinaColors.Darker, Offset(7f, 27f), Size(50f, 35f), CornerRadius(11f))
            drawPath(sPath, Color.White, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(SinaColors.Orange, 2f, Offset(32f, 44f))
            drawLine(SinaColors.Orange, Offset(32f, 44.5f), Offset(32f, 47.7f), 2f, StrokeCap.Round)
        }
    }
}
