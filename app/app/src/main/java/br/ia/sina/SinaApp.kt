package br.ia.sina

import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import br.ia.sina.data.api.toApiError

import android.app.Application
import br.ia.sina.data.AppPreferences
import br.ia.sina.data.DeviceRepository
import br.ia.sina.data.GuardianRepository
import br.ia.sina.data.LocalAlertStore
import br.ia.sina.data.api.SinaApi
import br.ia.sina.data.api.createSinaApi
import br.ia.sina.protection.AlertNotifier
import br.ia.sina.protection.ProtectionScheduler

class SinaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlertNotifier.createChannels(this)
        ProtectionScheduler.syncWithRole(this)
    }
}

class AppContainer(private val app: Application) {
    val prefs = AppPreferences(app)
    val alerts = LocalAlertStore(app)
    val api: SinaApi = createSinaApi { prefs.current().deviceToken }
    val devices = DeviceRepository(prefs, api)
    val guardian = GuardianRepository(app, prefs, api)

    suspend fun deleteAccount() {
        if (prefs.current().deviceToken != null) {
            try {
                api.deleteAccount()
            } catch (e: Exception) {
                if (e.toApiError()?.httpStatus != 401) throw e
            }
        }
        WorkManager.getInstance(app).cancelAllWork()
        NotificationManagerCompat.from(app).cancelAll()
        alerts.clear()
        guardian.clearExcerpts()
        prefs.clear()
    }
}

val android.content.Context.container: AppContainer get() = (applicationContext as SinaApp).container
