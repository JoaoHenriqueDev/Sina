package br.ia.sina.guardian

import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.ia.sina.R
import br.ia.sina.container
import br.ia.sina.data.MessageSource
import br.ia.sina.data.Role
import br.ia.sina.data.api.FcmTokenRequest
import br.ia.sina.data.formatTime
import br.ia.sina.protection.AlertNotifier
import br.ia.sina.ui.navigation.Routes
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant

class SinaMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            val c = applicationContext.container
            val state = c.prefs.current()
            if (state.role == Role.GUARDIAN && state.deviceToken != null) {
                runCatching { c.api.updateFcmToken(FcmTokenRequest(token)) }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        runBlocking {
            if (applicationContext.container.prefs.current().role != Role.GUARDIAN) return@runBlocking
            when (data["type"]) {
                "SCAM_ALERT" -> showScam(data)
                "PROTECTION_INACTIVE" -> showInactive(data)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showScam(data: Map<String, String>) {
        val alertId = data["alertId"] ?: return
        val c = applicationContext.container
        data["messageExcerpt"]?.takeIf { it.isNotBlank() }?.let { c.guardian.saveExcerpt(alertId, it) }
        if (!AlertNotifier.canPost(this)) return
        val protectedId = data["protectedDeviceId"].orEmpty()
        val name = c.guardian.nameOf(protectedId, data["protectedName"])
        val source = data["source"]?.let { runCatching { MessageSource.valueOf(it) }.getOrNull() }?.label ?: "mensagem"
        val label = data["scamLabel"] ?: "Golpe"
        val summary = data["familySummary"]?.takeIf { it.isNotBlank() }
        val text = listOfNotNull("$label no $source.", summary, "O aviso já apareceu no celular. Ligue para confirmar que não houve pagamento.")
            .joinToString(" ")
        val id = alertId.hashCode()
        val notification = NotificationCompat.Builder(this, AlertNotifier.CHANNEL_SCAM)
            .setSmallIcon(R.drawable.ic_stat_sina)
            .setColor(ContextCompat.getColor(this, R.color.sina_scam))
            .setContentTitle("$name recebeu um golpe")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(AlertNotifier.openRouteIntent(this, Routes.guardianAlert(alertId), id))
            .addAction(0, "Ver detalhes", AlertNotifier.openRouteIntent(this, Routes.guardianAlert(alertId), id + 1))
            .build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    @SuppressLint("MissingPermission")
    private suspend fun showInactive(data: Map<String, String>) {
        if (!AlertNotifier.canPost(this)) return
        val protectedId = data["protectedDeviceId"] ?: return
        val name = applicationContext.container.guardian.nameOf(protectedId, data["protectedName"])
        val since = data["lastSeenAt"]?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val text = (if (since != null) "O celular não manda sinal desde ${formatTime(since)}. " else "O celular parou de mandar sinal. ") +
            "Peça para abrir o Sina."
        val id = ("inactive$protectedId").hashCode()
        val notification = NotificationCompat.Builder(this, AlertNotifier.CHANNEL_INACTIVE)
            .setSmallIcon(R.drawable.ic_stat_sina)
            .setColor(ContextCompat.getColor(this, R.color.sina_suspicious))
            .setContentTitle("Proteção de $name parou")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(AlertNotifier.openRouteIntent(this, Routes.guardianInactive(protectedId), id))
            .addAction(0, "Ver o que fazer", AlertNotifier.openRouteIntent(this, Routes.guardianInactive(protectedId), id + 1))
            .build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }
}
