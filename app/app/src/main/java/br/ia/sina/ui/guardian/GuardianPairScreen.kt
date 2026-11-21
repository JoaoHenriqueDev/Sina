package br.ia.sina.ui.guardian

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import br.ia.sina.container
import br.ia.sina.data.ProtectedPerson
import br.ia.sina.data.api.toApiError
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.signup.FieldShape
import br.ia.sina.ui.signup.sinaFieldColors
import br.ia.sina.ui.theme.SinaColors
import br.ia.sina.ui.theme.SinaShapes
import kotlinx.coroutines.launch
import java.io.IOException


@Composable
fun GuardianPairScreen(onBack: (() -> Unit)?, onPaired: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var paired by remember { mutableStateOf<ProtectedPerson?>(null) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    LaunchedEffect(Unit) { if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA) }

    fun redeem(value: String) {
        if (submitting || paired != null || value.length != 6) return
        submitting = true
        error = null
        scope.launch {
            try {
                paired = c.guardian.redeem(value)
                    ?: ProtectedPerson("", "", "Pessoa protegida", true, null)
            } catch (e: IOException) {
                error = "Sem internet. Confira a conexão e tente de novo."
            } catch (e: Exception) {
                val api = e.toApiError() ?: throw e
                error = when (api.code) {
                    "TOO_MANY_GUARDIANS" -> "Esta pessoa já tem o máximo de familiares ligados."
                    "PAIRING_CODE_NOT_FOUND", "VALIDATION_ERROR" -> "Código não encontrado. Confira os 8 números."
                    "ALREADY_PAIRED" -> "Vocês já estão ligados."
                    "RATE_LIMITED" -> "Muitas tentativas. Espere um pouco e tente de novo."
                    else -> "Não foi possível ligar. Tente de novo."
                }
            } finally {
                submitting = false
            }
        }
    }

    DarkPage {
        if (onBack != null) TopBar("Escanear código", onBack) else Spacer(Modifier.height(64.dp))
        Text(
            buildAnnotatedString {
                append("No celular da pessoa protegida, toque em ")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) { append("Ligar familiar") }
            },
            style = SinaText.body(16, 23),
            color = SinaColors.OnDarkSubtitleStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                if (cameraGranted) {
                    QrScanner(
                        onCode = { scanned -> code = scanned; redeem(scanned) },
                        modifier = Modifier.padding(4.dp).matchParentSize().clip(RoundedCornerShape(16.dp)),
                    )
                } else {
                    TextButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
                        Text("Permitir câmera", style = SinaText.medium(16), color = Color.White, textAlign = TextAlign.Center)
                    }
                }
                ViewfinderCorners(Modifier.matchParentSize())
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(SinaColors.Sheet, SinaShapes.Sheet)
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Ou digite o código de 8 números", style = SinaText.medium(17), color = SinaColors.TextPrimary)
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter(Char::isDigit).take(8); error = null },
                label = { Text("Código") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                textStyle = SinaText.body(22, 28).copy(letterSpacing = 6.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { redeem(code) }),
                shape = FieldShape,
                colors = sinaFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            DarkButton(
                if (submitting) "Ligando…" else "Ligar",
                enabled = code.length == 8 && !submitting,
                onClick = { redeem(code) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    paired?.let { person -> NicknameDialog(person, onDone = { onPaired() }) }
}

@Composable
private fun NicknameDialog(person: ProtectedPerson, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var nickname by rememberSaveable { mutableStateOf(if (person.name == "Pessoa protegida") "" else person.name) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Ligado!") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(40) },
                label = { Text("Como você chama essa pessoa?") },
                placeholder = { Text("Ex.: Mãe, Vó Maria") },
                singleLine = true,
                shape = FieldShape,
                colors = sinaFieldColors(),
            )
        },
        containerColor = SinaColors.Sheet,
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (person.deviceId.isNotEmpty()) c.prefs.setNickname(person.deviceId, nickname)
                    onDone()
                }
            }) { Text("Continuar", color = SinaColors.TextPrimary) }
        },
    )
}
