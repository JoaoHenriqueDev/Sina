package br.ia.sina.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import br.ia.sina.data.api.SinaJson

enum class Role { PROTECTED, GUARDIAN }

data class Profile(val name: String, val email: String, val phone: String)

fun normalizePhone(raw: String): String? = raw.filter { it.isDigit() }.takeIf { it.length in 10..13 }

data class AppState(
    val onboardingDone: Boolean,
    val role: Role?,
    val consentAccepted: Boolean,
    val deviceToken: String?,
    val profile: Profile?,
    val nicknames: Map<String, String>,
)

private val Context.dataStore by preferencesDataStore("sina")

class AppPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    val state: Flow<AppState> = store.data.map { p ->
        AppState(
            onboardingDone = p[ONBOARDING_DONE] ?: false,
            role = p[ROLE]?.let { runCatching { Role.valueOf(it) }.getOrNull() },
            consentAccepted = p[CONSENT] ?: false,
            deviceToken = p[DEVICE_TOKEN],
            profile = run {
                val name = p[PROFILE_NAME]
                val email = p[PROFILE_EMAIL]
                val phone = p[PROFILE_PHONE]
                if (name != null && email != null && phone != null) Profile(name, email, phone) else null
            },
            nicknames = p[NICKNAMES]?.let { runCatching { SinaJson.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: emptyMap(),
        )
    }

    suspend fun current(): AppState = state.first()

    suspend fun setOnboardingDone() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setRole(role: Role) {
        store.edit {
            if (it[ROLE] != role.name) {
                it.remove(DEVICE_ID)
                it.remove(DEVICE_TOKEN)
                it.remove(CONSENT)
                it.remove(NICKNAMES)
            }
            it[ROLE] = role.name
        }
    }

    suspend fun setConsentAccepted() {
        store.edit { it[CONSENT] = true }
    }

    suspend fun saveProfile(profile: Profile) {
        store.edit {
            it[PROFILE_NAME] = profile.name
            it[PROFILE_EMAIL] = profile.email
            it[PROFILE_PHONE] = profile.phone
        }
    }

    suspend fun saveDevice(deviceId: String, deviceToken: String) {
        store.edit {
            it[DEVICE_ID] = deviceId
            it[DEVICE_TOKEN] = deviceToken
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    suspend fun setNickname(protectedId: String, nickname: String) {
        store.edit { p ->
            val current = p[NICKNAMES]?.let { runCatching { SinaJson.decodeFromString<Map<String, String>>(it) }.getOrNull() } ?: emptyMap()
            val name = nickname.trim()
            p[NICKNAMES] = SinaJson.encodeToString(if (name.isEmpty()) current - protectedId else current + (protectedId to name))
        }
    }

    private companion object {
        val NICKNAMES = stringPreferencesKey("nicknames")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ROLE = stringPreferencesKey("role")
        val CONSENT = booleanPreferencesKey("consent_accepted")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_TOKEN = stringPreferencesKey("device_token")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
        val PROFILE_PHONE = stringPreferencesKey("profile_phone")
    }
}
