package br.ia.sina.protection

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import br.ia.sina.container
import br.ia.sina.data.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SinaNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val batcher = MessageBatcher(scope, clock = System::currentTimeMillis) { batch ->
        val state = container.prefs.current()
        if (state.role == Role.PROTECTED && state.consentAccepted) {
            AnalysisWorker.enqueue(this, batch)
        }
    }
    private val recent = object : LinkedHashMap<Int, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Long>?) = size > 200
    }

    override fun onListenerConnected() {
        ProtectionScheduler.sendHeartbeatNow(this)
    }

    override fun onListenerDisconnected() {
        ProtectionScheduler.sendHeartbeatNow(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val source = MessageSources.resolve(this, sbn.packageName) ?: return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = messageText(extras)?.trim() ?: return
        if (text.length < 3 || isSummary(text)) return

        val key = (sbn.packageName + "|" + title + "|" + text).hashCode()
        val now = System.currentTimeMillis()
        synchronized(recent) {
            val last = recent[key]
            if (last != null && now - last < DEDUP_WINDOW_MS) return
            recent[key] = now
        }

        val isGroup = extras.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION) ||
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null
        val anonymized = Anonymizer.anonymize(text, title)
        val message = anonymized.copy(signals = anonymized.signals + listOfNotNull(Anonymizer.senderSignal(title, isGroup)))
        batcher.add("${sbn.packageName}|$title", source, message, sbn.postTime.takeIf { it > 0 } ?: now)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun messageText(extras: Bundle): String? {
        val messages = BundleCompat.getParcelableArray(extras, Notification.EXTRA_MESSAGES, Bundle::class.java)
        val last = (messages?.lastOrNull() as? Bundle)?.getCharSequence("text")?.toString()
        return last
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    }

    private fun isSummary(text: String): Boolean = summary.matches(text.lowercase())

    private companion object {
        const val DEDUP_WINDOW_MS = 10 * 60 * 1000L
        val summary = Regex("""^\d+\s+(novas\s+)?(mensagens|messages|new messages)( novas)?.*|^verificando novas mensagens.*|^checking for new messages.*""")
    }
}
