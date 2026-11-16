package br.ia.sina.protection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.ia.sina.MainActivity
import br.ia.sina.R
import br.ia.sina.data.LocalAlert
import br.ia.sina.data.Verdict
import br.ia.sina.data.verdictTitle
import br.ia.sina.ui.navigation.Routes

object AlertNotifier {
    const val CHANNEL_SCAM = "scam_alerts"
    const val CHANNEL_SUSPICIOUS = "suspicious_alerts"
    const val CHANNEL_INACTIVE = "protection_inactive"
    const val EXTRA_ROUTE = "br.ia.sina.ROUTE"
    const val EXTRA_NOTIFICATION_ID = "br.ia.sina.NOTIFICATION_ID"
    const val ACTION_DISMISS = "br.ia.sina.DISMISS"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_SCAM, "Alertas de golpe", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisa na hora quando uma mensagem é golpe"
                },
                NotificationChannel(CHANNEL_SUSPICIOUS, "Alertas de suspeita", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisa quando uma mensagem pode ser golpe"
                },
                NotificationChannel(CHANNEL_INACTIVE, "Proteção parada", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisa quando o Sina para de funcionar no celular de quem você protege"
                },
            ),
        )
    }

    fun openRouteIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun show(context: Context, alert: LocalAlert) {
        if (!canPost(context)) return
        val id = alert.id.hashCode()
        val scam = alert.verdict == Verdict.SCAM
        val title = verdictTitle(alert.verdict).removeSuffix(".")
        val text = alert.elderlyMessage?.removePrefix(verdictTitle(alert.verdict))?.trim()?.ifBlank { null }
            ?: if (scam) "Não faça o Pix. Ligue para seu familiar no número de sempre e confirme." else "Não abra o link nem pague nada."
        val dismiss = dismissIntent(context, id)
        val notification = NotificationCompat.Builder(context, if (scam) CHANNEL_SCAM else CHANNEL_SUSPICIOUS)
            .setSmallIcon(R.drawable.ic_stat_sina)
            .setColor(ContextCompat.getColor(context, if (scam) R.color.sina_scam else R.color.sina_suspicious))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openRouteIntent(context, Routes.elderAlert(alert.id), id))
            .addAction(0, "Ver aviso", openRouteIntent(context, Routes.elderAlert(alert.id), id + 1))
            .addAction(0, "Entendi", dismiss)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun dismissIntent(context: Context, notificationId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        Intent(context, DismissReceiver::class.java).setAction(ACTION_DISMISS).putExtra(EXTRA_NOTIFICATION_ID, notificationId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canPost(context: Context): Boolean = android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    class DismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
        }
    }
}
