package br.ia.sina.protection

import br.ia.sina.data.MessageSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MessageBatch(
    val source: MessageSource,
    val text: String,
    val signals: List<LocalSignal>,
    val firstReceivedAt: Long,
    val messageCount: Int,
    val context: String? = null,
)

class MessageBatcher(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val quietMs: Long = 10_000,
    private val maxWaitMs: Long = 30_000,
    private val maxMessages: Int = 10,
    private val contextMs: Long = 5 * 60_000,
    private val onFlush: suspend (MessageBatch) -> Unit,
) {
    private class Pending(val source: MessageSource, val firstReceivedAt: Long, val startedAt: Long) {
        val messages = mutableListOf<String>()
        val signals = linkedSetOf<LocalSignal>()
        var job: Job? = null
    }

    private val mutex = Mutex()
    private val pending = mutableMapOf<String, Pending>()
    private val history = mutableMapOf<String, MutableList<Pair<Long, String>>>()

    fun add(conversationKey: String, source: MessageSource, message: AnonymizedMessage, receivedAt: Long) {
        scope.launch {
            val ready = mutex.withLock {
                val now = clock()
                val p = pending.getOrPut(conversationKey) { Pending(source, receivedAt, now) }
                p.messages += message.text
                p.signals += message.signals
                p.job?.cancel()
                if (p.messages.size >= maxMessages) {
                    pending.remove(conversationKey)
                    return@withLock p.toBatch(conversationKey, now)
                }
                val wait = minOf(quietMs, maxWaitMs - (now - p.startedAt)).coerceAtLeast(0)
                p.job = scope.launch {
                    delay(wait)
                    val due = mutex.withLock {
                        pending.remove(conversationKey)?.takeIf { it === p }?.toBatch(conversationKey, clock())
                    }
                    if (due != null) onFlush(due)
                }
                null
            }
            if (ready != null) onFlush(ready)
        }
    }

    private fun Pending.toBatch(key: String, now: Long): MessageBatch {
        val kept = fitFromEnd(messages)
        history.values.forEach { h -> h.removeAll { now - it.first > contextMs } }
        history.values.removeAll { it.isEmpty() }
        val previous = fitFromEnd(history[key].orEmpty().map { it.second })
        history.getOrPut(key) { mutableListOf() } += messages.map { now to it }
        return MessageBatch(
            source = source,
            text = kept.joinToString("\n").take(Anonymizer.MAX_LENGTH),
            signals = signals.toList(),
            firstReceivedAt = firstReceivedAt,
            messageCount = kept.size,
            context = previous.joinToString("\n").takeLast(Anonymizer.MAX_LENGTH).ifEmpty { null },
        )
    }

    private fun fitFromEnd(messages: List<String>): List<String> {
        val kept = messages.toMutableList()
        while (kept.size > 1 && kept.joinToString("\n").length > Anonymizer.MAX_LENGTH) kept.removeAt(0)
        return kept
    }
}
