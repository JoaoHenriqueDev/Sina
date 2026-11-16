package br.ia.sina.protection

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.ia.sina.container
import br.ia.sina.data.LocalAlert
import br.ia.sina.data.MessageSource
import br.ia.sina.data.Verdict
import br.ia.sina.data.api.AnalyzeRequest
import br.ia.sina.data.api.HeartbeatRequest
import br.ia.sina.data.api.toApiError
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class AnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val source = inputData.getString(KEY_SOURCE)?.let { runCatching { MessageSource.valueOf(it) }.getOrNull() }
            ?: return Result.failure()
        val text = inputData.getString(KEY_TEXT) ?: return Result.failure()
        val signals = inputData.getStringArray(KEY_SIGNALS)?.toList().orEmpty()
        val receivedAt = inputData.getLong(KEY_RECEIVED_AT, System.currentTimeMillis())
        val messageCount = inputData.getInt(KEY_COUNT, 1).coerceIn(1, 50)
        val context = inputData.getString(KEY_CONTEXT)
        if (System.currentTimeMillis() - receivedAt > MAX_AGE_MS) return Result.success()

        val c = applicationContext.container
        if (c.prefs.current().let { it.deviceToken == null && it.profile == null }) return Result.success()
        return try {
            c.devices.ensureRegistered()
            val response = c.api.analyze(
                AnalyzeRequest(source.name, text, signals, Instant.ofEpochMilli(receivedAt).toString(), messageCount, context),
            )
            runCatching { c.api.heartbeat(HeartbeatRequest(ListenerStatus.isEnabled(applicationContext))) }
            val verdict = runCatching { Verdict.valueOf(response.verdict) }.getOrDefault(Verdict.SAFE)
            if (verdict != Verdict.SAFE) {
                val alert = LocalAlert(
                    id = UUID.randomUUID().toString(),
                    verdict = verdict,
                    scamType = response.scamType,
                    source = source,
                    receivedAtMillis = receivedAt,
                    elderlyMessage = response.elderlyMessage,
                    guardianNotified = response.guardianNotified,
                )
                c.alerts.add(alert)
                AlertNotifier.show(applicationContext, alert)
            }
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        } catch (e: Exception) {
            val api = e.toApiError() ?: throw e
            if ((api.httpStatus == 429 || api.httpStatus >= 500) && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val KEY_SOURCE = "source"
        private const val KEY_TEXT = "text"
        private const val KEY_SIGNALS = "signals"
        private const val KEY_RECEIVED_AT = "received_at"
        private const val KEY_COUNT = "message_count"
        private const val KEY_CONTEXT = "context"
        private const val MAX_ATTEMPTS = 5
        private const val MAX_AGE_MS = 30 * 60 * 1000L

        fun enqueue(context: Context, batch: MessageBatch) {
            val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setInputData(
                    workDataOf(
                        KEY_SOURCE to batch.source.name,
                        KEY_TEXT to batch.text,
                        KEY_SIGNALS to batch.signals.map { it.name }.toTypedArray(),
                        KEY_RECEIVED_AT to batch.firstReceivedAt,
                        KEY_COUNT to batch.messageCount,
                        KEY_CONTEXT to batch.context,
                    ),
                )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
