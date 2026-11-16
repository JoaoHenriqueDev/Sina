package br.ia.sina.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ia.sina.container
import br.ia.sina.data.ProtectedPerson
import br.ia.sina.data.Verdict
import br.ia.sina.data.api.AlertItem
import br.ia.sina.data.formatTime
import br.ia.sina.data.guardianAlertLabel
import br.ia.sina.ui.components.BrandBar
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.PlainButton
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SheetSection
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.VerdictIcon
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMM", Locale.forLanguageTag("pt-BR"))

private fun dayLabel(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> dayFormat.format(date)
    }
}

@Composable
fun GuardianAlertsScreen(onTab: (GuardianTab) -> Unit, onOpenAlert: (String) -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<ProtectedPerson>>(emptyList()) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyScams by rememberSaveable { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<AlertItem>?>(null) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { people = runCatching { c.guardian.people() }.getOrDefault(emptyList()) }
    LaunchedEffect(selected) {
        items = null
        runCatching { c.guardian.alerts(protectedId = selected) }
            .onSuccess { items = it.items; cursor = it.nextCursor; failed = false }
            .onFailure { failed = true }
    }

    val names = people.associate { it.deviceId to it.name }
    val visible = items.orEmpty().filter { !onlyScams || it.verdict == "SCAM" }

    DarkPage(bottomNav = { GuardianNavBar(GuardianTab.ALERTS, onTab) }) {
        BrandBar()
        HeroTitle("Alertas", size = 34, line = 40, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp))
        Sheet(PaddingValues(top = 24.dp, bottom = 16.dp), hasBottomNav = true) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                people.forEach { person ->
                    SinaChip(person.name, selected == person.deviceId) { selected = if (selected == person.deviceId) null else person.deviceId }
                }
                SinaChip("Só golpes", onlyScams) { onlyScams = !onlyScams }
            }
            when {
                failed && items == null -> Note("Sem internet. Tente de novo mais tarde.")
                items == null -> Note("Carregando…")
                visible.isEmpty() -> Note("Nenhum alerta por aqui.")
            }
            var lastDay: String? = null
            visible.forEach { alert ->
                val millis = alert.createdMillis()
                val day = dayLabel(millis)
                if (day != lastDay) {
                    SheetSection(day, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp))
                    lastDay = day
                }
                val verdict = if (alert.verdict == "SCAM") Verdict.SCAM else Verdict.SUSPICIOUS
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenAlert(alert.id) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    VerdictIcon(verdict)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            (if (verdict == Verdict.SCAM) "Golpe · " else "Suspeita · ") + guardianAlertLabel(alert.scamType),
                            style = SinaText.medium(16, 22.sp),
                            color = SinaColors.TextPrimary,
                        )
                        alert.familySummary?.let { Text(it, style = SinaText.body(14, 20), color = SinaColors.TextSecondary) }
                        Text(
                            "${names[alert.protectedDeviceId] ?: "Pessoa protegida"} · ${if (alert.source == "WHATSAPP") "WhatsApp" else "SMS"}",
                            style = SinaText.body(12, 17),
                            color = SinaColors.TextSecondary,
                        )
                    }
                    Text(formatTime(millis), style = SinaText.body(12, 17), color = SinaColors.TextSecondary)
                }
            }
            if (cursor != null) {
                PlainButton("Ver mais", modifier = Modifier.fillMaxWidth(), onClick = {
                    val next = cursor
                    scope.launch {
                        runCatching { c.guardian.alerts(protectedId = selected, cursor = next) }.onSuccess {
                            items = items.orEmpty() + it.items
                            cursor = it.nextCursor
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = SinaText.body(16, 24), color = SinaColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
}

@Composable
private fun SinaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(SinaColors.Dark) else Modifier.border(1.dp, SinaColors.Neutral, CircleShape))
            .toggleable(value = selected, onValueChange = { onClick() }, role = Role.Checkbox)
            .padding(start = if (selected) 10.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) LineIcon(LineIcons.Check, SinaColors.Orange, size = 18.dp, strokeWidth = 2.4f)
        Text(label, style = SinaText.Chip, color = if (selected) Color.White else SinaColors.TextSecondary)
    }
}
