package br.ia.sina.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.ia.sina.container
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.launch

private enum class DeleteStep { Idle, Confirm, Deleting, Failed }

@Composable
fun DeleteAccountItem(whatIsDeleted: String, onDeleted: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(DeleteStep.Idle) }

    SettingsItem("Excluir minha conta", "Apaga seus dados do Sina", onClick = { step = DeleteStep.Confirm }, titleColor = SinaColors.ScamStrong)

    when (step) {
        DeleteStep.Confirm, DeleteStep.Deleting -> AlertDialog(
            onDismissRequest = { if (step == DeleteStep.Confirm) step = DeleteStep.Idle },
            title = { Text("Excluir sua conta?") },
            text = { Text("$whatIsDeleted Isso não pode ser desfeito.") },
            confirmButton = {
                TextButton(enabled = step == DeleteStep.Confirm, onClick = {
                    step = DeleteStep.Deleting
                    scope.launch {
                        runCatching { c.deleteAccount() }
                            .onSuccess { step = DeleteStep.Idle; onDeleted() }
                            .onFailure { step = DeleteStep.Failed }
                    }
                }) { Text(if (step == DeleteStep.Deleting) "Excluindo…" else "Excluir", color = SinaColors.ScamStrong) }
            },
            dismissButton = {
                TextButton(enabled = step == DeleteStep.Confirm, onClick = { step = DeleteStep.Idle }) { Text("Cancelar", color = SinaColors.TextPrimary) }
            },
        )
        DeleteStep.Failed -> AlertDialog(
            onDismissRequest = { step = DeleteStep.Idle },
            title = { Text("Não deu certo") },
            text = { Text("Confira a internet e tente de novo.") },
            confirmButton = { TextButton(onClick = { step = DeleteStep.Idle }) { Text("Ok", color = SinaColors.TextPrimary) } },
        )
        DeleteStep.Idle -> Unit
    }
}
