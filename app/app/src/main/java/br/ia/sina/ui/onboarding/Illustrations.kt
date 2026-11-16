package br.ia.sina.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ia.sina.ui.theme.SinaColors

private val Raised = SinaColors.DarkRaised
private val Line = SinaColors.IllustrationLine
private val Orange = SinaColors.Orange
private val WarmFill = SinaColors.WarmFill
private val WarmLine = SinaColors.WarmLine
private val Ink = SinaColors.Dark

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, stroke: Color? = null, strokeWidth: Float = 0f) {
    drawRoundRect(fill, Offset(x, y), Size(w, h), CornerRadius(r))
    if (stroke != null) drawRoundRect(stroke, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(strokeWidth))
}

private fun svg(d: String) = PathParser().parsePathString(d).toPath()

@Composable
private fun Illustration(label: String, draw: DrawScope.() -> Unit) {
    Canvas(Modifier.widthIn(max = 320.dp).aspectRatio(320f / 280f).semantics { contentDescription = label }) {
        val k = size.width / 320f
        scale(k, pivot = Offset.Zero) { draw() }
    }
}

@Composable
fun ScamMessageIllustration() = Illustration("Mensagem de golpe sinalizada pelo Sina") {
    box(24f, 24f, 190f, 52f, 18f, Raised)
    box(44f, 42f, 120f, 8f, 4f, Line)
    box(44f, 58f, 80f, 8f, 4f, Line)
    box(50f, 106f, 230f, 84f, 20f, WarmFill, Orange, 2.5f)
    box(72f, 128f, 170f, 8f, 4f, WarmLine)
    box(72f, 146f, 140f, 8f, 4f, WarmLine)
    box(72f, 164f, 100f, 8f, 4f, WarmLine)
    drawCircle(Orange, 24f, Offset(272f, 106f))
    val s = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(svg("M272 94l-11 19h22z"), Ink, style = s)
    drawPath(svg("M272 101v5M272 110v.5"), Ink, style = s)
    box(24f, 214f, 150f, 44f, 16f, Raised)
    box(44f, 232f, 100f, 8f, 4f, Line)
}

@Composable
fun FamilyIllustration() = Illustration("Celular protegido avisando o celular de um familiar") {
    box(22f, 50f, 96f, 180f, 20f, Raised, Orange, 2.5f)
    translate(70f, 130f) {
        val s = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(svg("M0 -18l16 6v12c0 9-7 16.5-16 18-9-1.5-16-9-16-18v-12z"), Color.White, style = s)
        drawPath(svg("M-7 0l5 5 9-10"), Color.White, style = s)
    }
    box(202f, 50f, 96f, 180f, 20f, Raised, Orange, 2.5f)
    box(214f, 84f, 72f, 48f, 10f, WarmFill, Orange, 2f)
    box(224f, 96f, 44f, 6f, 3f, WarmLine)
    box(224f, 110f, 32f, 6f, 3f, WarmLine)
    box(214f, 148f, 72f, 8f, 4f, Line)
    box(214f, 164f, 52f, 8f, 4f, Line)
    drawPath(
        Path().apply { moveTo(118f, 140f); lineTo(140f, 140f); moveTo(180f, 140f); lineTo(202f, 140f) },
        Orange,
        style = Stroke(2.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 7f))),
    )
    drawCircle(Orange, 22f, Offset(160f, 140f))
    translate(160f, 140f) {
        val s = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(svg("M-7 4c0-5 1-10 7-10s7 5 7 10l2 2h-18z"), Ink, style = s)
        drawPath(svg("M-2 10a2 2 0 0 0 4 0"), Ink, style = s)
    }
}

@Composable
fun PrivacyIllustration() {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
    Illustration("Mensagem com nome, valor e código trocados por marcadores") {
        box(20f, 50f, 280f, 180f, 22f, Raised, Line, 2f)
        fun tag(x: Float, y: Float, w: Float, label: String) {
            box(x, y, w, 28f, 8f, Orange)
            val layout = measurer.measure(label, style.copy(fontSize = (13f / density / fontScale).sp))
            drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2f, y + (28f - layout.size.height) / 2f))
        }
        box(42f, 78f, 60f, 8f, 4f, Line)
        tag(112f, 68f, 78f, "[NOME]")
        box(200f, 78f, 60f, 8f, 4f, Line)
        box(42f, 118f, 90f, 8f, 4f, Line)
        tag(142f, 108f, 84f, "[VALOR]")
        box(236f, 118f, 44f, 8f, 4f, Line)
        box(42f, 158f, 70f, 8f, 4f, Line)
        tag(122f, 148f, 96f, "[CÓDIGO]")
        box(228f, 158f, 50f, 8f, 4f, Line)
        box(42f, 196f, 150f, 8f, 4f, Line)
    }
}
