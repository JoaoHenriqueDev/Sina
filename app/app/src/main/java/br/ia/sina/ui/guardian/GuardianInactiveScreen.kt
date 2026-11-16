package br.ia.sina.ui.guardian

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.ia.sina.container
import br.ia.sina.data.ProtectedPerson
import br.ia.sina.data.formatTime
import br.ia.sina.data.formatWhen
import br.ia.sina.ui.components.CircleBadge
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.dialPhone
import br.ia.sina.ui.components.Divider
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SheetSection
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.components.WhiteCard
import br.ia.sina.ui.theme.SinaColors


@Composable
fun GuardianInactiveScreen(protectedId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val person by produceState<ProtectedPerson?>(null, protectedId) {
        value = runCatching { c.guardian.person(protectedId) }.getOrNull()
    }
    val p = person
    val since = p?.lastSeenAt?.toEpochMilli()?.let {
        val day = formatWhen(it)
        when {
            day == "Ontem" -> "ontem, ${formatTime(it)}"
            day.contains("/") -> "$day, ${formatTime(it)}"
            else -> "hoje, $day"
        }
    }
    val shieldOff = "M4 4l16 16" to SinaColors.Orange
    DarkPage {
        TopBar(p?.name ?: "Pessoa protegida", onBack)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (p?.active == true) {
                CircleBadge(LineIcons.Shield, SinaColors.DarkRaised, Color.White, size = 72.dp, iconSize = 40.dp)
            } else {
                CircleBadge(LineIcons.Shield, SinaColors.DarkRaised, Color.White, size = 72.dp, iconSize = 40.dp, accent = shieldOff)
            }
            HeroTitle(if (p?.active == true) "Proteção ativa" else "Proteção desligada")
            val text = when {
                p == null -> null
                p.active -> "O sinal do celular de ${p.name} voltou. Está tudo certo."
                since != null -> "O celular de ${p.name} não manda sinal desde $since. Enquanto isso, golpes não são detectados."
                else -> "O celular de ${p.name} ainda não mandou sinal. Enquanto isso, golpes não são detectados."
            }
            if (text != null) Text(text, style = SinaText.body(16, 23), color = SinaColors.OnDarkSubtitle)
        }
        Sheet(PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp), gap = 16.dp) {
            SheetSection("Causas mais comuns", Modifier.padding(horizontal = 8.dp))
            WhiteCard {
                Cause(LineIcons.BellOff, "O acesso às notificações foi desligado")
                Divider()
                Cause(LineIcons.Battery, "A economia de bateria fechou o Sina")
                Divider()
                Cause(LineIcons.NoWifi, "Celular desligado ou sem internet")
            }
            Text(
                "Peça para ${p?.name ?: "a pessoa"} abrir o Sina. Este aviso some sozinho quando o sinal voltar.",
                style = SinaText.body(14, 20),
                color = SinaColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            val phone = p?.phone
            if (p != null && phone != null) {
                PushDown()
                DarkButton("Ligar para ${p.name}", onClick = { dialPhone(context, phone) }, modifier = Modifier.fillMaxWidth(), icon = LineIcons.Phone)
            }
        }
    }
}

@Composable
private fun Cause(icon: LineIcons, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LineIcon(icon, SinaColors.Dark, size = 22.dp)
        Text(text, style = SinaText.body(15, 21), color = SinaColors.TextPrimary, modifier = Modifier.weight(1f))
    }
}
