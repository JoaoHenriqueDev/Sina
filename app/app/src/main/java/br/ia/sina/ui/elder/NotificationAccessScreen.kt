package br.ia.sina.ui.elder

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.ia.sina.protection.ListenerStatus
import br.ia.sina.protection.openNotificationAccessSettings
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.PlainButton
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaLogo
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors

private val steps = listOf(
    "Toque em Abrir configurações.",
    "Encontre o Sina e ligue \"Permitir acesso a notificações\".",
    "Confirme tocando em Permitir.",
)

@Composable
fun NotificationAccessScreen(onGranted: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        if (ListenerStatus.isEnabled(context)) onGranted()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        openNotificationAccessSettings(context)
    }
    DarkPage {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SinaLogo(72.dp, withBackground = false)
            HeroTitle("Acesso às notificações")
            HeroText("Para conferir as mensagens, o Sina precisa ver as notificações deste celular.")
        }
        Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp), gap = 16.dp) {
            steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(SinaColors.OrangeLight, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = SinaText.medium(16), color = SinaColors.Dark)
                    }
                    Text(step, style = SinaText.body(16, 23), color = SinaColors.TextPrimary, modifier = Modifier.weight(1f))
                }
            }
            Row(
                Modifier.fillMaxWidth().background(SinaColors.OrangeLight, RoundedCornerShape(16.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LineIcon(LineIcons.Bell, SinaColors.Dark)
                Text(
                    "O Android vai avisar que o Sina poderá ler suas notificações. É assim que ele confere SMS e WhatsApp. Outras notificações são ignoradas.",
                    style = SinaText.body(14, 20),
                    color = SinaColors.Dark,
                )
            }
            PushDown()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkButton("Abrir configurações", onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationAccessSettings(context)
                    }
                }, modifier = Modifier.fillMaxWidth())
                PlainButton("Agora não", onClick = onSkip, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
