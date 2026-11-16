package br.ia.sina.ui.elder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.ia.sina.BuildConfig
import br.ia.sina.container
import br.ia.sina.data.api.PairingItem
import br.ia.sina.protection.BatteryStatus
import br.ia.sina.protection.ListenerStatus
import br.ia.sina.protection.openNotificationAccessSettings
import br.ia.sina.ui.components.AddRow
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.DeleteAccountItem
import br.ia.sina.ui.components.PersonRow
import br.ia.sina.ui.components.SettingsDivider
import br.ia.sina.ui.components.SettingsHeader
import br.ia.sina.ui.components.SettingsItem
import br.ia.sina.ui.components.SettingsNote
import br.ia.sina.ui.components.SettingsSwitch
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.role.PRIVACY_POLICY_URL
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.launch


@Composable
fun ElderSettingsScreen(onBack: () -> Unit, onLinkGuardian: () -> Unit, onAccountDeleted: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var pairings by remember { mutableStateOf<List<PairingItem>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var listenerOn by remember { mutableStateOf(ListenerStatus.isEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(BatteryStatus.isIgnoringOptimizations(context)) }
    var confirm by remember { mutableStateOf<PairingItem?>(null) }
    var unlinkError by remember { mutableStateOf(false) }

    LifecycleResumeEffect(reload) {
        listenerOn = ListenerStatus.isEnabled(context)
        batteryIgnored = BatteryStatus.isIgnoringOptimizations(context)
        val job = scope.launch {
            runCatching { c.devices.pairings() }
                .onSuccess { pairings = it; loadFailed = false }
                .onFailure { loadFailed = true }
        }
        onPauseOrDispose { job.cancel() }
    }

    DarkPage {
        TopBar("Ajustes", onBack, bottomPadding = 12.dp)
        Sheet(PaddingValues(top = 16.dp)) {
            SettingsHeader("Familiares que recebem avisos")
            when {
                loadFailed && pairings == null -> SettingsNote("Sem internet. Não foi possível carregar os familiares.")
                pairings == null -> SettingsNote("Carregando…")
                pairings!!.isEmpty() -> SettingsNote("Nenhum familiar ligado.")
            }
            pairings.orEmpty().forEach { item ->
                val name = item.guardian?.displayName ?: "Familiar"
                PersonRow(name, pairedSince(item.createdAt)?.let { "Ligado desde $it" }, "Desligar", onAction = { confirm = item })
            }
            AddRow("Ligar outro familiar", onLinkGuardian)
            SettingsDivider()
            SettingsHeader("Proteção")
            SettingsSwitch("Acesso às notificações", "Necessário para o Sina funcionar", listenerOn) { openNotificationAccessSettings(context) }
            SettingsSwitch("Ignorar otimização de bateria", "Evita que o Android feche o Sina", batteryIgnored) { BatteryStatus.requestIgnore(context) }
            SettingsDivider()
            SettingsItem("Política de privacidade", onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) })
            SettingsItem("Versão", BuildConfig.VERSION_NAME)
            SettingsDivider()
            DeleteAccountItem("Seu cadastro, os familiares ligados e os alertas são apagados do Sina.", onAccountDeleted)
        }
    }

    confirm?.let { item ->
        val name = item.guardian?.displayName ?: "Este familiar"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Desligar $name?") },
            text = { Text("$name vai parar de receber os avisos de golpe deste celular.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        runCatching { c.api.deletePairing(item.pairingId) }
                            .onSuccess { reload++ }
                            .onFailure { unlinkError = true }
                    }
                }) { Text("Desligar", color = SinaColors.ScamStrong) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar", color = SinaColors.TextPrimary) } },
        )
    }
    if (unlinkError) {
        AlertDialog(
            onDismissRequest = { unlinkError = false },
            title = { Text("Não deu certo") },
            text = { Text("Confira a internet e tente de novo.") },
            confirmButton = { TextButton(onClick = { unlinkError = false }) { Text("Ok", color = SinaColors.TextPrimary) } },
        )
    }
}

private fun pairedSince(createdAt: String?): String? = createdAt?.let {
    runCatching {
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .format(java.time.Instant.parse(it).atZone(java.time.ZoneId.systemDefault()))
    }.getOrNull()
}
