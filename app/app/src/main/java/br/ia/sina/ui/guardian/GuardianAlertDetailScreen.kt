package br.ia.sina.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.ia.sina.container
import br.ia.sina.data.api.AlertItem
import br.ia.sina.data.formatTime
import br.ia.sina.data.formatWhen
import br.ia.sina.data.guardianAlertLabel
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.dialPhone
import br.ia.sina.ui.components.Divider
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.Pill
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SheetSection
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.components.WhiteCard
import br.ia.sina.ui.theme.SinaColors
import kotlin.math.roundToInt


private sealed interface DetailLoad {
    data object Loading : DetailLoad
    data object Failed : DetailLoad
    data class Ready(val alert: AlertItem, val person: String, val excerpt: String?, val phone: String?) : DetailLoad
}

@Composable
fun GuardianAlertDetailScreen(alertId: String, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val load by produceState<DetailLoad>(DetailLoad.Loading, alertId) {
        value = runCatching {
            val alert = c.guardian.alert(alertId) ?: return@runCatching DetailLoad.Failed
            val person = c.guardian.person(alert.protectedDeviceId)
            val name = person?.name ?: c.guardian.nameOf(alert.protectedDeviceId, null)
            DetailLoad.Ready(alert, name, alert.messageExcerpt ?: c.guardian.excerpt(alertId), person?.phone)
        }.getOrDefault(DetailLoad.Failed)
    }
    val ready = load as? DetailLoad.Ready
    val scam = ready?.alert?.verdict != "SUSPICIOUS"
    DarkPage {
        TopBar(if (scam) "Alerta de golpe" else "Alerta de suspeita", onBack)
        when (val l = load) {
            DetailLoad.Loading, DetailLoad.Failed -> {
                Spacer(Modifier.height(32.dp))
                Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp)) {
                    Text(
                        if (l == DetailLoad.Loading) "Carregando…" else "Não foi possível abrir este alerta. Confira a internet e tente de novo.",
                        style = SinaText.body(16, 24),
                        color = SinaColors.TextSecondary,
                    )
                }
            }
            is DetailLoad.Ready -> DetailBody(l)
        }
    }
}

@Composable
private fun ColumnScope.DetailBody(l: DetailLoad.Ready) {
    val alert = l.alert
    val scam = alert.verdict == "SCAM"
    val millis = alert.createdMillis()
    val day = formatWhen(millis)
    val whenText = when {
        day == "Ontem" -> "Ontem, ${formatTime(millis)}"
        day.contains("/") -> "$day, ${formatTime(millis)}"
        else -> "Hoje, $day"
    }
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val certainty = (if (scam) "Golpe" else "Suspeita") + " · ${(alert.confidence * 100).roundToInt()}% de certeza"
        if (scam) {
            Pill(certainty, SinaColors.ScamContainer, SinaColors.OnScamContainer, icon = LineIcons.Warning, iconStroke = 2f)
        } else {
            Pill(certainty, SinaColors.SuspiciousContainer, SinaColors.OnSuspiciousContainer, icon = LineIcons.Alert, iconStroke = 2f)
        }
        HeroTitle(guardianAlertLabel(alert.scamType))
        alert.familySummary?.let { Text(it, style = SinaText.body(16, 23), color = SinaColors.OnDarkSubtitle) }
    }
    Sheet(PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp), gap = 18.dp) {
        WhiteCard {
            InfoRow("Pessoa", l.person)
            Divider()
            InfoRow("Recebida por", if (alert.source == "WHATSAPP") "WhatsApp" else "SMS")
            Divider()
            InfoRow("Quando", whenText)
        }
        if (l.excerpt != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetSection("Trecho da mensagem", Modifier.padding(horizontal = 4.dp))
                Text(
                    l.excerpt,
                    style = SinaText.body(15, 22),
                    color = SinaColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth().background(SinaColors.CardSelected, RoundedCornerShape(16.dp)).padding(16.dp),
                )
                Text(
                    "Nomes, valores e números foram removidos no celular de ${l.person}.",
                    style = SinaText.body(12, 16),
                    color = SinaColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetSection("O que fazer", Modifier.padding(horizontal = 4.dp))
            Column(Modifier.padding(start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Ligue para ${l.person} e confirme que não houve pagamento nem envio de código.",
                    "Se houve pagamento por Pix, peça ao banco a devolução pelo MED do Pix.",
                ).forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}.", style = SinaText.body(15, 22), color = SinaColors.TextPrimary, modifier = Modifier.width(20.dp))
                        Text(step, style = SinaText.body(15, 22), color = SinaColors.TextPrimary)
                    }
                }
            }
        }
        if (l.phone != null) {
            val context = LocalContext.current
            DarkButton("Ligar para ${l.person}", onClick = { dialPhone(context, l.phone) }, modifier = Modifier.fillMaxWidth(), icon = LineIcons.Phone)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SinaText.body(15, 21), color = SinaColors.TextSecondary)
        Text(value, style = SinaText.body(15, 21), color = SinaColors.TextPrimary)
    }
}
