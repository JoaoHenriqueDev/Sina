package br.ia.sina.protection

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

object ListenerStatus {
    fun isEnabled(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
}

fun openNotificationAccessSettings(context: Context) {
    val component = android.content.ComponentName(context, SinaNotificationListener::class.java).flattenToString()
    val detail = if (android.os.Build.VERSION.SDK_INT >= 30) {
        android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
    } else {
        null
    }
    val list = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    for (intent in listOfNotNull(detail, list)) {
        try {
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: android.content.ActivityNotFoundException) {
        }
    }
}

object BatteryStatus {
    fun isIgnoringOptimizations(context: Context): Boolean =
        context.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    @android.annotation.SuppressLint("BatteryLife")
    fun requestIgnore(context: Context) {
        val request = android.content.Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri(),
        )
        try {
            context.startActivity(request.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
