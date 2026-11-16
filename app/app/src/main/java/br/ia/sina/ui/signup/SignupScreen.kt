package br.ia.sina.ui.signup

import android.Manifest
import android.os.Build
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import br.ia.sina.container
import br.ia.sina.data.Profile
import br.ia.sina.data.Role
import br.ia.sina.data.normalizePhone
import br.ia.sina.guardian.currentFcmToken
import br.ia.sina.ui.components.DarkButton
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.HeroText
import br.ia.sina.ui.components.LineIcon
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.theme.SinaColors
import java.io.IOException
import kotlinx.coroutines.launch

@Composable
fun sinaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SinaColors.Dark,
    focusedLabelColor = SinaColors.Dark,
    cursorColor = SinaColors.Dark,
    unfocusedBorderColor = SinaColors.Neutral,
    unfocusedLabelColor = SinaColors.TextSecondary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedSupportingTextColor = SinaColors.TextSecondary,
    unfocusedSupportingTextColor = SinaColors.TextSecondary,
    errorBorderColor = SinaColors.ScamStrong,
    errorLabelColor = SinaColors.ScamStrong,
    errorSupportingTextColor = SinaColors.ScamStrong,
    errorContainerColor = Color.Transparent,
)

val FieldShape = RoundedCornerShape(12.dp)

@Composable
fun SignupScreen(role: Role, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val guardian = role == Role.GUARDIAN
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var phoneError by rememberSaveable { mutableStateOf(false) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun register() {
        submitting = true
        error = null
        scope.launch {
            try {
                c.prefs.saveProfile(Profile(name.trim(), email.trim(), normalizePhone(phone)!!))
                c.devices.ensureRegistered(fcmToken = if (guardian) currentFcmToken() else null)
                onDone()
            } catch (e: IOException) {
                error = "Sem internet. Confira a conexão e tente de novo."
            } catch (e: Exception) {
                error = "Não foi possível continuar. Confira os dados e tente de novo."
            } finally {
                submitting = false
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { register() }

    DarkPage {
        TopBar("Seus dados", onBack)
        HeroText(
            if (guardian) "Você recebe uma notificação neste celular e um e-mail quando a pessoa protegida receber um golpe."
            else "Seu familiar vê seu nome quando receber um alerta de golpe deste celular.",
            Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
        )
        Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 36.dp), gap = 20.dp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Seu nome") },
                supportingText = { Text(if (guardian) "Aparece no celular de quem você proteger" else "Aparece para o familiar que você ligar") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                textStyle = SinaText.body(16, 24),
                shape = FieldShape,
                colors = sinaFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(254); emailError = false },
                label = { Text("E-mail") },
                supportingText = {
                    Text(
                        when {
                            emailError -> "Confira o e-mail digitado"
                            guardian -> "Também recebe os alertas de golpe. Nada de propaganda."
                            else -> "Só para assuntos da sua conta. Nada de propaganda."
                        },
                    )
                },
                isError = emailError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                textStyle = SinaText.body(16, 24),
                shape = FieldShape,
                colors = sinaFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch in " +()-" }.take(20); phoneError = false },
                label = { Text("Telefone com DDD") },
                supportingText = { Text(if (phoneError) "Confira o número com DDD" else "Ex.: (11) 98765-4321") },
                isError = phoneError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                textStyle = SinaText.body(16, 24),
                shape = FieldShape,
                colors = sinaFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().background(SinaColors.OrangeLight, RoundedCornerShape(16.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LineIcon(LineIcons.Bell, SinaColors.Dark)
                Text(
                    buildAnnotatedString {
                        append("Ao continuar, o Android vai perguntar se o Sina pode enviar notificações. Toque em ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Permitir") }
                        append(".")
                    },
                    style = SinaText.body(14, 20),
                    color = SinaColors.Dark,
                )
            }
            PushDown()
            if (error != null) {
                Text(error!!, style = SinaText.body(14, 20), color = SinaColors.ScamStrong)
            }
            DarkButton(
                if (submitting) "Aguarde…" else "Continuar",
                enabled = name.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && !submitting,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    emailError = !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                    phoneError = normalizePhone(phone) == null
                    if (!emailError && !phoneError) {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else register()
                    }
                },
            )
        }
    }
}
