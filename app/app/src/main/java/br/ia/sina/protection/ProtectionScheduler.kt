package br.ia.sina.protection

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.ia.sina.container
import br.ia.sina.data.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object ProtectionScheduler {
    private const val PERIODIC = "heartbeat"
    private const val NOW = "heartbeat-now"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun syncWithRole(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val state = app.container.prefs.current()
            if (state.role == Role.PROTECTED && state.consentAccepted) start(app) else stop(app)
        }
    }

    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).setConstraints(network).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        sendHeartbeatNow(context)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    fun sendHeartbeatNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<HeartbeatWorker>().setConstraints(network).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }
}
