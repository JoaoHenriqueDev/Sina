package br.ia.sina.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymizerTest {

    private fun anon(text: String, sender: String? = null) = Anonymizer.anonymize(text, sender)

    @Test
    fun falseRelativeWithPix() {
        val r = anon("Oi mãe, mudei de número. Me faz um Pix de R$ 1.500,00 urgente pra essa chave 11 98765-4321")
        assertEquals("Oi mãe, mudei de número. Me faz um Pix de [VALOR] urgente pra essa chave [TELEFONE]", r.text)
        assertTrue(LocalSignal.PIX_REQUEST in r.signals)
        assertTrue(LocalSignal.URGENCY in r.signals)
    }

    @Test
    fun phoneFormats() {
        assertEquals("ligue [TELEFONE]", anon("ligue +55 (11) 98765-4321").text)
        assertEquals("ligue [TELEFONE]", anon("ligue (21)3456-7890").text)
        assertEquals("ligue [TELEFONE]", anon("ligue 11987654321").text)
    }

    @Test
    fun linksKeepOnlyDomainAndFlagShorteners() {
        val r = anon("Seu pacote está retido. Pague a taxa em https://bit.ly/3xYz?cpf=12345678900")
        assertEquals("Seu pacote está retido. Pague a taxa em [LINK:bit.ly]", r.text)
        assertTrue(LocalSignal.SHORTENED_LINK in r.signals)
        assertEquals("Acesse [LINK:ent-rastreio.top] hoje", anon("Acesse ent-rastreio.top/br/maria hoje").text)
        assertEquals("veja [LINK:caixa.gov.br]", anon("veja www.caixa.gov.br/beneficio").text)
    }

    @Test
    fun sentenceWithoutSpaceIsNotLink() {
        assertEquals("Ok.Vamos amanhã", anon("Ok.Vamos amanhã").text)
    }

    @Test
    fun verificationCodes() {
        val r = anon("Seu código de verificação é 483920. Não compartilhe com ninguém.")
        assertEquals("Seu código de verificação é [CODIGO]. Não compartilhe com ninguém.", r.text)
        assertFalse(LocalSignal.CODE_REQUEST in r.signals)
        val ask = anon("Me passa o código que chegou aí por SMS, 6 números")
        assertTrue(LocalSignal.CODE_REQUEST in ask.signals)
        assertTrue(LocalSignal.CODE_REQUEST in anon("Informe sua senha para desbloquear").signals)
    }

    @Test
    fun datesAndTimesAreKept() {
        assertEquals("consulta dia 10/11/2026 às 14:20", anon("consulta dia 10/11/2026 às 14:20").text)
    }

    @Test
    fun cpfEmailAndValues() {
        assertEquals("CPF [CPF] e [EMAIL]", anon("CPF 123.456.789-00 e maria.silva@gmail.com").text)
        assertEquals("deve [VALOR] e mais [VALOR]", anon("deve 250,90 e mais 300 reais").text)
        assertEquals("ganhou [VALOR]", anon("ganhou R$5000").text)
    }

    @Test
    fun senderNameIsReplaced() {
        val r = anon("Maria, aqui é do banco. Dona Maria Souza precisa confirmar", sender = "Maria Souza")
        assertEquals("[NOME], aqui é do banco. Dona [NOME] precisa confirmar", r.text)
    }

    @Test
    fun senderThatIsPhoneIsIgnoredAsName() {
        assertEquals("oi", anon("oi", sender = "+55 11 98765-4321").text)
    }

    @Test
    fun truncatesToApiLimit() {
        assertEquals(Anonymizer.MAX_LENGTH, anon("a ".repeat(2000)).text.length)
    }

    @Test
    fun plainMessageHasNoSignals() {
        val r = anon("Vamos almoçar no domingo?")
        assertEquals("Vamos almoçar no domingo?", r.text)
        assertTrue(r.signals.isEmpty())
    }

    @Test
    fun senderSignalFromNotificationTitle() {
        assertEquals(LocalSignal.UNKNOWN_SENDER, Anonymizer.senderSignal("+55 11 98765-4321", false))
        assertEquals(LocalSignal.UNKNOWN_SENDER, Anonymizer.senderSignal("29045", false))
        assertEquals(LocalSignal.UNKNOWN_SENDER, Anonymizer.senderSignal("~Fernanda", false))
        assertEquals(LocalSignal.SAVED_CONTACT, Anonymizer.senderSignal("Maria Filha", false))
        assertEquals(null, Anonymizer.senderSignal("Família", true))
        assertEquals(null, Anonymizer.senderSignal(null, false))
    }
}
