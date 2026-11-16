package br.ia.sina.protection

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.ia.sina.container
import br.ia.sina.data.Role
import br.ia.sina.data.api.HeartbeatRequest
import java.io.IOException

class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val state = c.prefs.current()
        if (state.role != Role.PROTECTED || !state.consentAccepted) return Result.success()
        if (state.deviceToken == null && state.profile == null) return Result.success()
        return try {
            c.devices.ensureRegistered()
            c.api.heartbeat(HeartbeatRequest(ListenerStatus.isEnabled(applicationContext)))
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.success()
        }
    }
}
