package br.ia.sina.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ia.sina.ui.components.SinaLogo
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(Unit) {
        delay(1500)
        finish()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(SinaColors.Dark)
            .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SinaLogo(size = 136.dp, withBackground = false)
        Text("Sina", style = SinaText.medium(32).copy(letterSpacing = 0.5.sp), color = Color.White)
    }
}
