package br.ia.sina.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.ia.sina.data.Verdict
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes

@Composable
fun VerdictIcon(verdict: Verdict, modifier: Modifier = Modifier, size: Dp = 40.dp, iconSize: Dp = 22.dp, stroke: Float = 2f) {
    val (bg, fg) = when (verdict) {
        Verdict.SCAM -> SinaColors.ScamContainer to SinaColors.ScamIcon
        Verdict.SUSPICIOUS -> SinaColors.SuspiciousContainer to SinaColors.SuspiciousIcon
        Verdict.SAFE -> SinaColors.SafeContainer to SinaColors.OnSafeContainer
    }
    Box(modifier.size(size).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        LineIcon(if (verdict == Verdict.SCAM) LineIcons.Warning else LineIcons.Alert, fg, size = iconSize, strokeWidth = stroke)
    }
}
