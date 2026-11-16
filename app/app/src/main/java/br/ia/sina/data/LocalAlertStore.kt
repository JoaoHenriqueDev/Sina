package br.ia.sina.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.ia.sina.data.api.SinaJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class LocalAlert(
    val id: String,
    val verdict: Verdict,
    val scamType: String?,
    val source: MessageSource,
    val receivedAtMillis: Long,
    val elderlyMessage: String?,
    val guardianNotified: Boolean,
)

enum class MessageSource(val label: String) { SMS("SMS"), WHATSAPP("WhatsApp") }

private val Context.alertStore by preferencesDataStore("alerts")

// shortcut: JSON list in DataStore capped at MAX_ALERTS, move to Room if history needs queries or paging
class LocalAlertStore(context: Context) {
    private val store = context.applicationContext.alertStore

    val alerts: Flow<List<LocalAlert>> = store.data.map { p ->
        p[KEY]?.let { runCatching { SinaJson.decodeFromString<List<LocalAlert>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun get(id: String): LocalAlert? = alerts.first().firstOrNull { it.id == id }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    suspend fun add(alert: LocalAlert) {
        store.edit { p ->
            val current = p[KEY]?.let { runCatching { SinaJson.decodeFromString<List<LocalAlert>>(it) }.getOrNull() } ?: emptyList()
            val updated = (listOf(alert) + current.filterNot { it.id == alert.id }).take(MAX_ALERTS)
            p[KEY] = SinaJson.encodeToString(updated)
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("items")
        const val MAX_ALERTS = 50
    }
}
