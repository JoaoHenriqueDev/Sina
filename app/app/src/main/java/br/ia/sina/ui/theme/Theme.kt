package br.ia.sina.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SinaColorScheme = lightColorScheme(
    primary = SinaColors.Dark,
    onPrimary = Color.White,
    primaryContainer = SinaColors.CardSelected,
    onPrimaryContainer = SinaColors.TextPrimary,
    secondary = SinaColors.Orange,
    onSecondary = Color.White,
    secondaryContainer = SinaColors.OrangeLight,
    onSecondaryContainer = SinaColors.TextPrimary,
    background = SinaColors.Sheet,
    onBackground = SinaColors.TextPrimary,
    surface = SinaColors.Sheet,
    onSurface = SinaColors.TextPrimary,
    surfaceVariant = SinaColors.CardSelected,
    onSurfaceVariant = SinaColors.TextSecondary,
    outline = SinaColors.Neutral,
    outlineVariant = SinaColors.CardBorder,
    error = SinaColors.ScamStrong,
    onError = Color.White,
    errorContainer = SinaColors.ScamContainer,
    onErrorContainer = SinaColors.OnScamContainer,
)

object SinaShapes {
    val Button = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(20.dp)
    val Sheet = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
}

@Composable
fun SinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SinaColorScheme,
        typography = Typography,
        shapes = Shapes(medium = SinaShapes.Card, large = SinaShapes.Card),
        content = content,
    )
}
