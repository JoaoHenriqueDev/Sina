package br.ia.sina.ui.guardian

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.ia.sina.BuildConfig
import br.ia.sina.container
import br.ia.sina.data.ProtectedPerson
import br.ia.sina.protection.AlertNotifier
import br.ia.sina.ui.components.BrandBar
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.DeleteAccountItem
import br.ia.sina.ui.components.HeroTitle
import br.ia.sina.ui.components.PersonRow
import br.ia.sina.ui.components.SettingsDivider
import br.ia.sina.ui.components.SettingsHeader
import br.ia.sina.ui.components.SettingsItem
import br.ia.sina.ui.components.SettingsNote
import br.ia.sina.ui.components.SettingsSwitch
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.role.PRIVACY_POLICY_URL
import br.ia.sina.ui.signup.FieldShape
import br.ia.sina.ui.signup.sinaFieldColors
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.launch


@Composable
fun GuardianSettingsScreen(onTab: (GuardianTab) -> Unit, onAccountDeleted: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<ProtectedPerson>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var notificationsOn by remember { mutableStateOf(AlertNotifier.canPost(context)) }
    var unlink by remember { mutableStateOf<ProtectedPerson?>(null) }
    var rename by remember { mutableStateOf<ProtectedPerson?>(null) }
    var actionFailed by remember { mutableStateOf(false) }

    LifecycleResumeEffect(reload) {
        notificationsOn = AlertNotifier.canPost(context)
        val job = scope.launch {
            runCatching { c.guardian.people() }.onSuccess { people = it; failed = false }.onFailure { failed = true }
        }
        onPauseOrDispose { job.cancel() }
    }

    DarkPage(bottomNav = { GuardianNavBar(GuardianTab.SETTINGS, onTab) }) {
        BrandBar()
        HeroTitle("Ajustes", size = 34, line = 40, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp))
        Sheet(PaddingValues(top = 16.dp, bottom = 16.dp), hasBottomNav = true) {
            SettingsHeader("Pessoas que você protege")
            when {
                failed && people == null -> SettingsNote("Sem internet. Não foi possível carregar.")
                people == null -> SettingsNote("Carregando…")
                people!!.isEmpty() -> SettingsNote("Ninguém ligado ainda.")
            }
            people.orEmpty().forEach { person ->
                PersonRow(person.name, "Toque para mudar o nome", "Desligar", onAction = { unlink = person }, onClick = { rename = person })
            }
            SettingsDivider()
            SettingsHeader("Avisos")
            SettingsSwitch("Notificações", "Necessárias para receber os alertas", notificationsOn) {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            SettingsDivider()
            SettingsItem("Política de privacidade", onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) })
            SettingsItem("Versão", BuildConfig.VERSION_NAME)
            SettingsDivider()
            DeleteAccountItem("Seu cadastro e as ligações com as pessoas que você protege são apagados do Sina.", onAccountDeleted)
        }
    }

    unlink?.let { person ->
        AlertDialog(
            onDismissRequest = { unlink = null },
            title = { Text("Desligar ${person.name}?") },
            text = { Text("Você vai parar de receber os alertas de golpe de ${person.name}.") },
            confirmButton = {
                TextButton(onClick = {
                    unlink = null
                    scope.launch {
                        runCatching { c.api.deletePairing(person.pairingId) }.onSuccess { reload++ }.onFailure { actionFailed = true }
                    }
                }) { Text("Desligar", color = SinaColors.ScamStrong) }
            },
            dismissButton = { TextButton(onClick = { unlink = null }) { Text("Cancelar", color = SinaColors.TextPrimary) } },
        )
    }
    rename?.let { person ->
        var value by remember(person) { mutableStateOf(person.name) }
        AlertDialog(
            onDismissRequest = { rename = null },
            title = { Text("Como você chama essa pessoa?") },
            text = {
                OutlinedTextField(value = value, onValueChange = { value = it.take(40) }, singleLine = true, shape = FieldShape, colors = sinaFieldColors())
            },
            confirmButton = {
                TextButton(onClick = {
                    rename = null
                    scope.launch { c.prefs.setNickname(person.deviceId, value); reload++ }
                }) { Text("Salvar", color = SinaColors.TextPrimary) }
            },
            dismissButton = { TextButton(onClick = { rename = null }) { Text("Cancelar", color = SinaColors.TextPrimary) } },
        )
    }
    if (actionFailed) {
        AlertDialog(
            onDismissRequest = { actionFailed = false },
            title = { Text("Não deu certo") },
            text = { Text("Confira a internet e tente de novo.") },
            confirmButton = { TextButton(onClick = { actionFailed = false }) { Text("Ok", color = SinaColors.TextPrimary) } },
        )
    }
}
