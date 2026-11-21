package br.ia.sina.ui.elder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ia.sina.container
import br.ia.sina.data.api.toApiError
import br.ia.sina.ui.components.DarkPage
import br.ia.sina.ui.components.LineIcons
import br.ia.sina.ui.components.OutlineDarkButton
import br.ia.sina.ui.components.Pill
import br.ia.sina.ui.components.PushDown
import br.ia.sina.ui.components.QrCode
import br.ia.sina.ui.components.Sheet
import br.ia.sina.ui.components.SinaText
import br.ia.sina.ui.components.TopBar
import br.ia.sina.ui.theme.SinaColors
import kotlinx.coroutines.delay
import java.io.IOException
import java.time.Instant

private data class ActiveCode(val code: String, val expiresAtMillis: Long)

@Composable
fun PairingCodeScreen(onBack: () -> Unit, onPaired: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val paired by rememberUpdatedState(onPaired)
    var generation by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf<ActiveCode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(generation) {
        code = null
        error = null
        try {
            c.devices.ensureRegistered()
            val r = c.api.createPairingCode()
            code = ActiveCode(r.code, Instant.parse(r.expiresAt).toEpochMilli())
        } catch (e: IOException) {
            error = "Sem internet. Confira a conexão e tente de novo."
        } catch (e: Exception) {
            val api = e.toApiError() ?: throw e
            error = if (api.httpStatus == 429) "Muitos códigos em pouco tempo. Espere um pouco e tente de novo." else "Não foi possível gerar o código. Tente de novo."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        val initial = runCatching { c.devices.pairings().size }.getOrNull()
        while (initial != null) {
            delay(4000)
            val count = runCatching { c.devices.pairings().size }.getOrNull() ?: continue
            if (count > initial) {
                Toast.makeText(context, "Familiar ligado", Toast.LENGTH_LONG).show()
                paired()
                break
            }
        }
    }

    val current = code
    val remaining = current?.let { ((it.expiresAtMillis - now) / 1000).coerceAtLeast(0) }
    val expired = remaining == 0L

    DarkPage {
        TopBar("Ligar familiar", onBack)
        Text(
            buildAnnotatedString {
                append("No celular do seu familiar, abra o Sina, toque em ")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) { append("Proteger um familiar") }
                append(" e aponte a câmera para o código.")
            },
            style = SinaText.HeroBody,
            color = SinaColors.OnDarkSubtitle,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 28.dp),
        )
        Sheet(PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp), gap = 20.dp, horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                error != null -> Text(error!!, style = SinaText.body(16, 24), color = SinaColors.ScamStrong)
                current == null -> Box(Modifier.height(260.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SinaColors.Orange) }
                else -> {
                    Box(
                        Modifier
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .border(1.dp, SinaColors.CardBorder, RoundedCornerShape(24.dp))
                            .padding(20.dp),
                    ) {
                        QrCode(current.code, 220.dp, "QR code do código ${current.code.toList().joinToString(" ")}")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ou digite o código", style = SinaText.body(15, 21), color = SinaColors.TextSecondary)
                        Text(
                            current.code.chunked(4).joinToString(" "),
                            style = SinaText.medium(44).copy(fontFamily = FontFamily.Monospace, letterSpacing = 6.sp),
                            color = if (expired) SinaColors.Neutral else SinaColors.TextPrimary,
                            modifier = Modifier.semantics { contentDescription = current.code.toList().joinToString(" ") },
                        )
                    }
                    if (expired) {
                        Pill("Código expirado. Gere um novo.", SinaColors.ScamContainer, SinaColors.OnScamContainer, icon = LineIcons.Clock, iconStroke = 2f)
                    } else {
                        Pill(
                            "Expira em ${remaining!! / 60}:${"%02d".format(remaining % 60)} · uso único",
                            SinaColors.OrangeLight,
                            SinaColors.Dark,
                            icon = LineIcons.Clock,
                            iconStroke = 2f,
                        )
                    }
                }
            }
            PushDown()
            OutlineDarkButton(
                if (error != null) "Tentar de novo" else "Gerar novo código",
                onClick = { generation++ },
                enabled = current != null || error != null,
            )
        }
    }
}
