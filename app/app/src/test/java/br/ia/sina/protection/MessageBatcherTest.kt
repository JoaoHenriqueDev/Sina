package br.ia.sina.protection

import br.ia.sina.data.MessageSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageBatcherTest {

    private fun TestScope.batcher(out: MutableList<MessageBatch>) =
        MessageBatcher(backgroundScope, clock = { testScheduler.currentTime }) { out += it }

    private fun msg(text: String, vararg signals: LocalSignal) = AnonymizedMessage(text, signals.toList())

    @Test
    fun groupsMessagesArrivingWithinTenSeconds() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        b.add("wa|Fulano", MessageSource.WHATSAPP, msg("Oi mãe, mudei de número"), 1000)
        advanceTimeBy(6_000)
        b.add("wa|Fulano", MessageSource.WHATSAPP, msg("Me faz um Pix de [VALOR] urgente", LocalSignal.PIX_REQUEST, LocalSignal.URGENCY), 7000)
        advanceTimeBy(9_000)
        runCurrent()
        assertTrue(out.isEmpty())
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(1, out.size)
        assertEquals("Oi mãe, mudei de número\nMe faz um Pix de [VALOR] urgente", out[0].text)
        assertEquals(2, out[0].messageCount)
        assertEquals(1000L, out[0].firstReceivedAt)
        assertEquals(listOf(LocalSignal.PIX_REQUEST, LocalSignal.URGENCY), out[0].signals)
    }

    @Test
    fun separateConversationsAreNotMixed() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        b.add("wa|A", MessageSource.WHATSAPP, msg("a"), 0)
        b.add("sms|B", MessageSource.SMS, msg("b"), 0)
        advanceTimeBy(10_100)
        runCurrent()
        assertEquals(setOf("a", "b"), out.map { it.text }.toSet())
    }

    @Test
    fun flushesAtMaxWaitEvenIfMessagesKeepComing() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        repeat(8) { i ->
            b.add("wa|A", MessageSource.WHATSAPP, msg("m$i"), 0)
            advanceTimeBy(5_000)
        }
        runCurrent()
        assertEquals(1, out.size)
        assertTrue(out[0].messageCount in 6..7)
    }

    @Test
    fun flushesImmediatelyAtMaxMessages() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        repeat(10) { b.add("wa|A", MessageSource.WHATSAPP, msg("m$it"), 0) }
        runCurrent()
        assertEquals(1, out.size)
        assertEquals(10, out[0].messageCount)
    }

    @Test
    fun keepsMostRecentMessagesWithinApiLimit() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        b.add("wa|A", MessageSource.WHATSAPP, msg("x".repeat(995)), 0)
        b.add("wa|A", MessageSource.WHATSAPP, msg("pede o Pix"), 0)
        advanceTimeBy(10_100)
        runCurrent()
        assertEquals("pede o Pix", out[0].text)
        assertEquals(1, out[0].messageCount)
    }

    @Test
    fun previousBatchesOfSameConversationGoAsContext() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        b.add("wa|A", MessageSource.WHATSAPP, msg("Meu celular caiu na água"), 0)
        advanceTimeBy(15_000)
        b.add("wa|B", MessageSource.WHATSAPP, msg("Outra conversa"), 0)
        b.add("wa|A", MessageSource.WHATSAPP, msg("Me ajuda com uma coisinha?"), 0)
        advanceTimeBy(10_100)
        runCurrent()
        assertEquals(3, out.size)
        assertEquals(null, out[0].context)
        assertEquals("Meu celular caiu na água", out.last { it.text.startsWith("Me ajuda") }.context)
        assertEquals(null, out.first { it.text == "Outra conversa" }.context)
    }

    @Test
    fun contextExpiresAfterFiveMinutes() = runTest {
        val out = mutableListOf<MessageBatch>()
        val b = batcher(out)
        b.add("wa|A", MessageSource.WHATSAPP, msg("antiga"), 0)
        advanceTimeBy(6 * 60_000)
        b.add("wa|A", MessageSource.WHATSAPP, msg("nova"), 0)
        advanceTimeBy(10_100)
        runCurrent()
        assertEquals(null, out.last().context)
    }
}
