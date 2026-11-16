package br.ia.sina.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DotProgress
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val text: String,
    val illustration: @Composable () -> Unit,
)

private val pages = listOf(
    OnboardingPage(
        "Golpe não passa batido",
        "O Sina confere SMS e WhatsApp e avisa quando uma mensagem parece golpe.",
    ) { ScamMessageIllustration() },
    OnboardingPage(
        "Sua família sabe na hora",
        "Um familiar recebe o aviso assim que um golpe chega, para ajudar sem demora.",
    ) { FamilyIllustration() },
    OnboardingPage(
        "Seus dados ficam com você",
        "Nomes, valores e códigos viram marcadores antes da análise. Nenhuma mensagem é guardada.",
    ) { PrivacyIllustration() },
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    HorizontalPager(pagerState) { index ->
        val page = pages[index]
        val last = index == pages.lastIndex
        Column(Modifier.fillMaxSize().background(SinaColors.Dark)) {
            Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { page.illustration() }
                HeroTitle(page.title)
                HeroText(page.text, Modifier.padding(top = 8.dp))
                DotProgress(pages.size, index, Modifier.padding(top = 20.dp))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SinaColors.Sheet, SinaShapes.Sheet)
                    .navigationBarsPadding()
                    .padding(PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp)),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Bem-vindo ao Sina", style = SinaText.medium(22, SinaText.TopBar.lineHeight), color = SinaColors.TextPrimary)
                    Text("Cadastro rápido, sem senha.", style = SinaText.HeroBody, color = SinaColors.TextSecondary)
                }
                DarkButton(
                    if (last) "Começar" else "Continuar",
                    onClick = { if (last) onFinish() else scope.launch { pagerState.animateScrollToPage(index + 1) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (last) {
                    Spacer(Modifier.height(48.dp))
                } else {
                    TextButton(
                        onClick = onFinish,
                        colors = ButtonDefaults.textButtonColors(contentColor = SinaColors.Dark),
                        modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pular", style = SinaText.medium(15), color = SinaColors.Dark)
                            Spacer(Modifier.width(8.dp))
                            LineIcon(LineIcons.ArrowRight, SinaColors.Dark, size = 20.dp)
                        }
                    }
                }
            }
        }
    }
}
