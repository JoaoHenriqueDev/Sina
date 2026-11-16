package br.ia.sina.data

import br.ia.sina.data.api.PairingItem
import br.ia.sina.data.api.RegisterRequest
import br.ia.sina.data.api.SinaApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeviceRepository(private val prefs: AppPreferences, private val api: SinaApi) {
    private val mutex = Mutex()

    suspend fun ensureRegistered(fcmToken: String? = null) = mutex.withLock {
        val state = prefs.current()
        val role = state.role ?: error("Papel não escolhido")
        if (state.deviceToken != null) return@withLock
        val profile = state.profile ?: error("Cadastro incompleto")
        val response = api.register(
            RegisterRequest(role = role.name, fcmToken = fcmToken, displayName = profile.name, email = profile.email, phone = profile.phone),
        )
        prefs.saveDevice(response.deviceId, response.deviceToken)
    }

    suspend fun isRegistered(): Boolean = prefs.current().deviceToken != null

    suspend fun pairings(): List<PairingItem> {
        ensureRegistered()
        return api.pairings().items
    }
}
