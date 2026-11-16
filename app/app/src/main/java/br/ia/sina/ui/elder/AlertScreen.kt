package br.ia.sina.ui.elder

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.ia.sina.container
import br.ia.sina.data.LocalAlert
import br.ia.sina.data.MessageSource
import br.ia.sina.data.Verdict
import br.ia.sina.data.formatTime
import br.ia.sina.data.joinNames
import br.ia.sina.data.scamTypeDescription
import br.ia.sina.data.verdictTitle
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.Pill
import br.ia.sina.ui.components.PlainButton
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.components.VerdictIcon
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes

private sealed interface AlertLoad {
    data object Loading : AlertLoad
    data object Missing : AlertLoad
    data class Ready(val alert: LocalAlert) : AlertLoad
}

@Composable
fun AlertScreen(alertId: String, onDone: () -> Unit, onAskHelp: () -> Unit) {
    val context = LocalContext.current
    val load by produceState<AlertLoad>(AlertLoad.Loading, alertId) {
        value = context.container.alerts.get(alertId)?.let { AlertLoad.Ready(it) } ?: AlertLoad.Missing
    }
    when (val l = load) {
        AlertLoad.Loading -> DarkPage { TopBar("Aviso", onDone, close = true) }
        AlertLoad.Missing -> DarkPage {
            TopBar("Aviso", onDone, close = true)
            HeroTitle("Aviso não encontrado", modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 36.dp))
            Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp), gap = 20.dp) {
                Text("Este aviso não está mais neste celular.", style = SinaText.body(16, 24), color = SinaColors.TextSecondary)
                PushDown()
                DarkButton("Voltar ao início", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
        is AlertLoad.Ready -> AlertContent(l.alert, onDone, onAskHelp, openContacts = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
    }
}

@Composable
private fun AlertContent(alert: LocalAlert, onDone: () -> Unit, onAskHelp: () -> Unit, openContacts: () -> Unit) {
    val context = LocalContext.current
    val scam = alert.verdict == Verdict.SCAM
    val title = verdictTitle(alert.verdict)
    val message = alert.elderlyMessage?.removePrefix(title)?.trim()?.ifBlank { null }
        ?: if (scam) {
            "Não faça o Pix. Ligue para seu familiar no número de sempre e confirme."
        } else {
            "Não abra o link nem pague nada. Se espera uma entrega, fale com a loja pelo contato oficial."
        }
    val notified by produceState<String?>(null, alert.id) {
        if (scam && alert.guardianNotified) {
            val names = runCatching { context.container.devices.pairings().mapNotNull { it.guardian?.displayName } }.getOrNull().orEmpty()
            value = when (names.size) {
                0 -> "Seu familiar foi avisado"
                1 -> "${names[0]} foi avisado"
                else -> "${joinNames(names)} foram avisados"
            }
        }
    }
    val sourceLine = if (alert.source == MessageSource.SMS) "SMS" else "Mensagem do ${alert.source.label}"
    DarkPage {
        TopBar("Aviso", onDone, close = true)
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VerdictIcon(alert.verdict, size = 72.dp, iconSize = 40.dp, stroke = 2.2f)
            if (scam) HeroTitle(title, size = 40, line = 48) else HeroTitle(title, size = 36, line = 44)
            HeroText(message, large = true)
        }
        Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp), gap = 20.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, SinaShapes.Card)
                    .border(1.dp, SinaColors.CardBorder, SinaShapes.Card)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("$sourceLine · ${formatTime(alert.receivedAtMillis)}", style = SinaText.medium(13), color = SinaColors.TextSecondary)
                Text(scamTypeDescription(alert.scamType), style = SinaText.body(16, 23), color = SinaColors.TextPrimary)
            }
            if (notified != null) {
                Pill(notified!!, SinaColors.SafeContainer, SinaColors.OnSafeContainer, icon = LineIcons.Check)
            }
            if (!scam) {
                Text("Avisos de suspeita não são enviados ao seu familiar.", style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
            }
            PushDown()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (scam) {
                    DarkButton("Entendi, não vou pagar", onClick = onDone, modifier = Modifier.fillMaxWidth())
                    PlainButton("Abrir contatos", onClick = openContacts, modifier = Modifier.fillMaxWidth())
                } else {
                    DarkButton("Entendi", onClick = onDone, modifier = Modifier.fillMaxWidth())
                    PlainButton("Pedir ajuda a um familiar", onClick = onAskHelp, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
