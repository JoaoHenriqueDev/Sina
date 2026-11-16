package br.ia.sina.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.ia.sina.data.api.AlertItem
import br.ia.sina.data.api.AlertPage
import br.ia.sina.data.api.PairingItem
import br.ia.sina.data.api.RedeemRequest
import br.ia.sina.data.api.SinaApi
import br.ia.sina.data.api.SinaJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class ProtectedPerson(
    val pairingId: String,
    val deviceId: String,
    val name: String,
    val active: Boolean,
    val lastSeenAt: Instant?,
    val phone: String? = null,
)

private val Context.excerptStore by preferencesDataStore("guardian_excerpts")

class GuardianRepository(context: Context, private val prefs: AppPreferences, private val api: SinaApi) {
    private val excerpts = context.applicationContext.excerptStore
    private val alertCache = ConcurrentHashMap<String, AlertItem>()

    suspend fun people(): List<ProtectedPerson> {
        val nicknames = prefs.current().nicknames
        return api.pairings().items.mapNotNull { it.toPerson(nicknames) }
    }

    suspend fun person(deviceId: String): ProtectedPerson? = people().firstOrNull { it.deviceId == deviceId }

    suspend fun nameOf(deviceId: String, fallback: String?): String =
        prefs.current().nicknames[deviceId] ?: fallback?.takeIf { it.isNotBlank() } ?: "Pessoa protegida"

    suspend fun redeem(code: String): ProtectedPerson? =
        api.redeem(RedeemRequest(code)).toPerson(prefs.current().nicknames)

    suspend fun alerts(protectedId: String? = null, cursor: String? = null, limit: Int = 20): AlertPage =
        api.alerts(protectedId, limit, cursor).also { page -> page.items.forEach { alertCache[it.id] = it } }

    suspend fun alert(id: String): AlertItem? {
        alertCache[id]?.let { return it }
        var cursor: String? = null
        repeat(3) {
            val page = alerts(cursor = cursor, limit = 50)
            page.items.firstOrNull { it.id == id }?.let { return it }
            cursor = page.nextCursor ?: return null
        }
        return null
    }

    suspend fun clearExcerpts() {
        excerpts.edit { it.clear() }
    }

    suspend fun saveExcerpt(alertId: String, excerpt: String) {
        excerpts.edit { p ->
            val current = p[EXCERPTS]?.let { runCatching { SinaJson.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: emptyMap()
            p[EXCERPTS] = SinaJson.encodeToString((current + (alertId to excerpt)).entries.toList().takeLast(MAX_EXCERPTS).associate { it.key to it.value })
        }
    }

    suspend fun excerpt(alertId: String): String? = excerpts.data.map { p ->
        p[EXCERPTS]?.let { runCatching { SinaJson.decodeFromString<Map<String, String>>(it) }.getOrNull() }?.get(alertId)
    }.first()

    private fun PairingItem.toPerson(nicknames: Map<String, String>): ProtectedPerson? {
        val peer = protectedDevice ?: return null
        return ProtectedPerson(
            pairingId = pairingId,
            deviceId = peer.deviceId,
            name = nicknames[peer.deviceId] ?: peer.displayName?.takeIf { it.isNotBlank() } ?: "Pessoa protegida",
            active = peer.protectionStatus == "ACTIVE",
            lastSeenAt = peer.lastSeenAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            phone = peer.phone,
        )
    }

    private companion object {
        val EXCERPTS = stringPreferencesKey("items")
        const val MAX_EXCERPTS = 100
    }
}

fun formatLastSeen(lastSeen: Instant?, now: Instant = Instant.now()): String {
    if (lastSeen == null) return "Ainda sem sinal"
    val minutes = Duration.between(lastSeen, now).toMinutes()
    if (minutes < 1) return "Último sinal agora"
    if (minutes < 60) return "Último sinal há $minutes min"
    val millis = lastSeen.toEpochMilli()
    val day = formatWhen(millis, now.toEpochMilli())
    return when {
        day == "Ontem" -> "Último sinal ontem às ${formatTime(millis)}"
        day.contains("/") -> "Último sinal em $day às ${formatTime(millis)}"
        else -> "Último sinal às $day"
    }
}

fun guardianAlertLabel(scamType: String?): String = scamTypeLabel(scamType).replaceFirstChar { it.uppercase() }
