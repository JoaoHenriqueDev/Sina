package br.ia.sina.ui.elder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import br.ia.sina.ui.components.CircleBadge
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.PlainButton
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.role.PRIVACY_POLICY_URL
import br.ia.sina.ui.theme.SinaColors

private data class ConsentItem(val icon: LineIcons, val title: String, val text: String)

private val items = listOf(
    ConsentItem(LineIcons.PersonAdd, "Seu cadastro", "Nome, e-mail e telefone ficam guardados no Sina para identificar você e ligar seu familiar."),
    ConsentItem(LineIcons.Lock, "Dados das mensagens ficam no celular", "Nomes, números, valores e códigos são trocados por marcadores antes do envio."),
    ConsentItem(LineIcons.Sparkle, "Análise por inteligência artificial", "O texto anonimizado é analisado pela OpenAI, que não o guarda."),
    ConsentItem(
        LineIcons.Bell,
        "Seu familiar é avisado",
        "Em caso de golpe, ele recebe o tipo, um resumo e um trecho sem dados pessoais, por notificação (Firebase, do Google) e e-mail (Resend).",
    ),
    ConsentItem(LineIcons.NoDatabase, "Nada de texto guardado", "Guardamos só o resultado da análise, nunca a mensagem."),
)

@Composable
fun ConsentScreen(onAccept: () -> Unit, onBack: () -> Unit) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    DarkPage {
        TopBar("Privacidade", onBack)
        HeroText(
            "Para proteger você, o Sina lê o texto das notificações de SMS e WhatsApp deste celular.",
            Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
        )
        Sheet(PaddingValues(top = 20.dp)) {
            items.forEach { item ->
                Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircleBadge(item.icon, SinaColors.OrangeLight, SinaColors.Dark)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, style = SinaText.medium(16), color = SinaColors.TextPrimary)
                        Text(item.text, style = SinaText.body(15, 21), color = SinaColors.TextSecondary)
                    }
                }
            }
            Text(
                "Ler a política de privacidade",
                style = SinaText.medium(15).copy(textDecoration = TextDecoration.Underline),
                color = SinaColors.Dark,
                modifier = Modifier
                    .clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
            PushDown()
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = agreed, onValueChange = { agreed = it }, role = Role.Checkbox)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = agreed,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = SinaColors.Dark, uncheckedColor = SinaColors.Neutral),
                    modifier = Modifier.size(20.dp),
                )
                Text("Li e concordo com o uso descrito acima", style = SinaText.body(16, 22), color = SinaColors.TextPrimary)
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainButton("Agora não", onClick = onBack)
                DarkButton("Ativar proteção", onClick = onAccept, enabled = agreed)
            }
        }
    }
}
