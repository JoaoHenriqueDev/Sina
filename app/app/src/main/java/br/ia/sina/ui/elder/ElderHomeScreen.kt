package br.ia.sina.ui.elder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.ia.sina.container
import br.ia.sina.data.alertListTitle
import br.ia.sina.data.formatWhen
import br.ia.sina.data.joinNames
import br.ia.sina.protection.ListenerStatus
import br.ia.sina.protection.ProtectionScheduler
import br.ia.sina.ui.components.BrandBar
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.Divider
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.Pill
import br.ia.sina.ui.components.PlainButton
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SheetSection
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.VerdictIcon
import br.ia.sina.ui.components.WhiteCard
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.launch

@Composable
fun ElderHomeScreen(
    onOpenAlert: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onLinkGuardian: () -> Unit,
    onEnableAccess: () -> Unit,
) {
    val context = LocalContext.current
    val c = context.container
    val alerts by c.alerts.alerts.collectAsState(initial = emptyList())
    var listenerOn by remember { mutableStateOf(ListenerStatus.isEnabled(context)) }
    var guardians by remember { mutableStateOf<List<String>?>(null) }
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        listenerOn = ListenerStatus.isEnabled(context)
        ProtectionScheduler.sendHeartbeatNow(context)
        val job = scope.launch {
            guardians = runCatching { c.devices.pairings().map { it.guardian?.displayName ?: "Seu familiar" } }.getOrNull()
        }
        onPauseOrDispose { job.cancel() }
    }

    val subtitle = when {
        !listenerOn -> "O Sina não consegue ver as notificações. Ative o acesso para voltar a proteger você."
        guardians == null -> null
        guardians!!.isEmpty() -> "Ligue um familiar para ele receber os avisos de golpe."
        guardians!!.size == 1 -> "${guardians!![0]} recebe os avisos de golpe."
        else -> "${joinNames(guardians!!)} recebem os avisos de golpe."
    }

    DarkPage {
        BrandBar(onSettings = onOpenSettings)
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (listenerOn) {
                Pill("Proteção ativa", SinaColors.SafeContainer, SinaColors.OnSafeContainer, icon = LineIcons.Check, iconStroke = 2.4f, iconSize = 16.dp)
            } else {
                Pill("Proteção desligada", SinaColors.SuspiciousContainer, SinaColors.OnSuspiciousContainer, icon = LineIcons.Alert, iconSize = 16.dp)
            }
            HeroTitle(if (listenerOn) "Conferindo SMS e WhatsApp" else "Proteção desligada", size = 34, line = 40)
            if (subtitle != null) HeroText(subtitle)
        }
        Sheet(PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp), gap = 16.dp) {
            SheetSection("Avisos recentes", Modifier.padding(horizontal = 8.dp))
            if (alerts.isEmpty()) {
                Text(
                    "Nenhum aviso até agora. Quando uma mensagem parecer golpe, ela aparece aqui.",
                    style = SinaText.body(16, 24),
                    color = SinaColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                WhiteCard {
                    alerts.forEachIndexed { i, alert ->
                        if (i > 0) Divider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAlert(alert.id) }
                                .heightIn(min = 72.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            VerdictIcon(alert.verdict)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(alertListTitle(alert.verdict, alert.scamType), style = SinaText.body(17, 22), color = SinaColors.TextPrimary)
                                Text(alert.source.label, style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
                            }
                            Text(formatWhen(alert.receivedAtMillis), style = SinaText.body(13, 18), color = SinaColors.TextSecondary)
                        }
                    }
                }
            }
            Text("Os avisos ficam só neste celular.", style = SinaText.body(13, 18), color = SinaColors.TextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
            PushDown()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!listenerOn) {
                    DarkButton("Ativar acesso às notificações", onClick = onEnableAccess, modifier = Modifier.fillMaxWidth())
                    PlainButton("Ligar familiar", onClick = onLinkGuardian, modifier = Modifier.fillMaxWidth())
                } else {
                    DarkButton("Ligar familiar", onClick = onLinkGuardian, modifier = Modifier.fillMaxWidth(), icon = LineIcons.PersonAdd)
                }
            }
        }
    }
}
