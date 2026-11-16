package br.ia.sina.ui.role

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import br.ia.sina.data.Role
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaLogo
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes

const val PRIVACY_POLICY_URL = "https://sina.ia.br/privacidade"

@Composable
fun RoleScreen(onContinue: (Role) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(Role.PROTECTED) }
    DarkPage {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SinaLogo(88.dp, withBackground = false)
            HeroTitle("Bem-vindo ao Sina")
            HeroText("O Sina avisa quando uma mensagem de SMS ou WhatsApp parece golpe. Quem vai usar este celular?")
        }
        Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp), gap = 24.dp) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleCard("Quero ser protegido", "O Sina confere as mensagens que chegam neste celular", selected == Role.PROTECTED) {
                    selected = Role.PROTECTED
                }
                RoleCard("Quero proteger um familiar", "Você recebe um aviso quando ele receber golpe", selected == Role.GUARDIAN) {
                    selected = Role.GUARDIAN
                }
            }
            PushDown()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DarkButton("Continuar", onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth())
                Text(
                    buildAnnotatedString {
                        append("Cadastro rápido, sem senha. ")
                        withLink(
                            LinkAnnotation.Url(
                                PRIVACY_POLICY_URL,
                                TextLinkStyles(SpanStyle(color = SinaColors.Dark, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)),
                            ),
                        ) { append("Privacidade") }
                    },
                    style = SinaText.body(13, 18),
                    color = SinaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RoleCard(title: String, text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) SinaColors.CardSelected else SinaColors.Card, SinaShapes.Card)
            .border(if (selected) 2.dp else 1.dp, if (selected) SinaColors.Dark else SinaColors.Outline, SinaShapes.Card)
            .selectable(selected = selected, onClick = onSelect, role = SemanticsRole.RadioButton)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = SinaColors.Dark, unselectedColor = SinaColors.Neutral),
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SinaText.medium(18), color = SinaColors.TextPrimary)
            Text(text, style = SinaText.body(15, 21), color = SinaColors.TextSecondary)
        }
    }
}
