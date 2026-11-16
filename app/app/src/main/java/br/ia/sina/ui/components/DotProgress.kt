package br.ia.sina.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ia.sina.ui.theme.SinaColors

@Composable
fun DotProgress(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Passo ${current + 1} de $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .background(if (i == current) SinaColors.Orange else SinaColors.IllustrationLine, CircleShape),
            )
        }
    }
}
