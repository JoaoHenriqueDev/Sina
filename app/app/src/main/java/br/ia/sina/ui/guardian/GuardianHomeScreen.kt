package br.ia.sina.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.ia.sina.container
import br.ia.sina.data.ProtectedPerson
import br.ia.sina.data.api.AlertItem
import br.ia.sina.data.formatLastSeen
import br.ia.sina.data.formatTime
import br.ia.sina.data.scamTypeLabel
import br.ia.sina.ui.components.Avatar
import br.ia.sina.ui.components.BrandBar
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.Pill
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


private data class HomeData(val people: List<ProtectedPerson>, val alerts: List<AlertItem>)

fun AlertItem.createdMillis(): Long = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L)

fun AlertItem.isToday(): Boolean =
    Instant.ofEpochMilli(createdMillis()).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()

@Composable
fun GuardianHomeScreen(
    onTab: (GuardianTab) -> Unit,
    onAddPerson: () -> Unit,
    onOpenAlert: (String) -> Unit,
    onOpenInactive: (String) -> Unit,
) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<HomeData?>(null) }
    var failed by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            runCatching {
                val people = c.guardian.people()
                val alerts = if (people.isEmpty()) emptyList() else c.guardian.alerts(limit = 30).items
                HomeData(people, alerts)
            }.onSuccess { data = it; failed = false }.onFailure { failed = true }
        }
        onPauseOrDispose { job.cancel() }
    }

    val d = data
    val todayCount = d?.alerts?.count { it.verdict == "SCAM" && it.isToday() } ?: 0
    val inactiveCount = d?.people?.count { !it.active } ?: 0
    val summary = when {
        d == null && failed -> "Sem internet. Mostrando quando a conexão voltar."
        d == null -> null
        d.people.isEmpty() -> "Ninguém ligado ainda."
        else -> listOf(
            when (todayCount) { 0 -> "Nenhum alerta hoje"; 1 -> "1 alerta hoje"; else -> "$todayCount alertas hoje" },
            when (inactiveCount) { 0 -> null; 1 -> "1 proteção inativa"; else -> "$inactiveCount proteções inativas" },
        ).filterNotNull().joinToString(" · ")
    }

    DarkPage(bottomNav = { GuardianNavBar(GuardianTab.PEOPLE, onTab, alertBadge = todayCount) }) {
        BrandBar()
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroTitle("Pessoas protegidas", size = 34, line = 40)
            if (summary != null) HeroText(summary)
        }
        Sheet(PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp), gap = 12.dp, hasBottomNav = true) {
            if (d != null && d.people.isEmpty()) {
                Text(
                    "Toque em Proteger alguém e escaneie o código no celular da pessoa que você quer proteger.",
                    style = SinaText.body(16, 24),
                    color = SinaColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            d?.people?.forEach { person ->
                val latest = d.alerts.firstOrNull { it.protectedDeviceId == person.deviceId && it.verdict == "SCAM" && it.isToday() }
                PersonCard(person, latest, onOpenAlert, onOpenInactive)
            }
            PushDown()
            DarkButton("Proteger alguém", onClick = onAddPerson, modifier = Modifier.fillMaxWidth(), icon = LineIcons.Plus)
        }
    }
}

@Composable
private fun PersonCard(person: ProtectedPerson, latest: AlertItem?, onOpenAlert: (String) -> Unit, onOpenInactive: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(SinaShapes.Card)
            .background(Color.White)
            .border(1.dp, SinaColors.CardBorder, SinaShapes.Card),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(person.name, 48.dp, if (person.active) SinaColors.OrangeLight else SinaColors.CardBorder, if (person.active) SinaColors.Dark else SinaColors.TextSecondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(person.name, style = SinaText.medium(18), color = SinaColors.TextPrimary)
                Text(formatLastSeen(person.lastSeenAt), style = SinaText.body(14, 20), color = SinaColors.TextSecondary)
            }
            if (person.active) {
                Pill("Ativa", SinaColors.SafeContainer, SinaColors.OnSafeContainer, dot = Color(0xFF1B6D3A))
            } else {
                Pill("Inativa", Color.Transparent, SinaColors.TextSecondary, dot = SinaColors.Neutral, border = SinaColors.Neutral)
            }
        }
        if (latest != null) {
            CardFooter(
                "Golpe hoje, ${formatTime(latest.createdMillis())} · ${scamTypeLabel(latest.scamType)}",
                SinaColors.ScamContainer,
                SinaColors.OnScamContainer,
                LineIcons.Warning to SinaColors.ScamIcon,
            ) { onOpenAlert(latest.id) }
        }
        if (!person.active) {
            CardFooter("O Sina parou de funcionar no celular", SinaColors.CardSelected, SinaColors.TextPrimary, null, SinaColors.TextSecondary) {
                onOpenInactive(person.deviceId)
            }
        }
    }
}

@Composable
private fun CardFooter(
    text: String,
    background: Color,
    content: Color,
    icon: Pair<LineIcons, Color>?,
    chevron: Color = content,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(background)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) LineIcon(icon.first, icon.second, size = 20.dp)
        Text(text, style = SinaText.body(15, 21), color = content, modifier = Modifier.weight(1f))
        LineIcon(LineIcons.Chevron, chevron, size = 20.dp)
    }
}
