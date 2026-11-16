package br.ia.sina.protection

import java.text.Normalizer

enum class LocalSignal { SHORTENED_LINK, URGENCY, PIX_REQUEST, CODE_REQUEST, UNKNOWN_SENDER, SAVED_CONTACT }

data class AnonymizedMessage(val text: String, val signals: List<LocalSignal>)

object Anonymizer {
    const val MAX_LENGTH = 1000

    private val shorteners = setOf(
        "bit.ly", "tinyurl.com", "cutt.ly", "is.gd", "t.co", "goo.gl", "rb.gy", "shorturl.at",
        "tiny.cc", "ow.ly", "s.id", "encurtador.com.br", "abre.ai", "l1nk.dev", "v.gd", "t.ly",
    )

    private const val TLDS =
        "com|br|net|org|info|biz|top|xyz|site|online|link|click|shop|store|app|io|me|ly|co|cc|gd|at|id|ai|dev|live|vip|club|icu|cfd|sbs|lat|pro|gov|edu|us|tk|ml|ga|cf|gq|pw|ws|in|ru|cn|to|gl"

    private val email = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
    private val url = Regex(
        """(?i)\b(?:https?://)?(?:www\.)?((?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+(?:$TLDS))\b(?![.\w-]*@)(?:[/?#][^\s]*)?""",
    )
    private val cpf = Regex("""(?<!\d)\d{3}\.\d{3}\.\d{3}-\d{2}(?!\d)""")
    private val money = Regex(
        """(?i)R\$\s?\d+(?:\.\d{3})*(?:,\d{1,2})?|(?<![\d.,])\d{1,3}(?:\.\d{3})*,\d{2}(?![\d,])|(?<!\d)\d+(?:[.,]\d+)?\s?(?:reais|mil reais)\b""",
    )
    private val phone = Regex("""(?<![\d\w])(?:\+?55[\s-]?)?(?:\(?\d{2}\)?[\s-]?)?9?\s?\d{4}[\s-]?\d{4}(?!\d)""")
    private val code = Regex("""(?<![\d/:\[])\d{4,}(?![\d/])""")
    private val spaces = Regex("""[ \t]+""")

    fun senderSignal(title: String?, isGroup: Boolean): LocalSignal? {
        val t = title?.trim().orEmpty()
        if (t.isEmpty() || isGroup) return null
        val digits = t.filter { it.isDigit() }
        val onlyNumber = digits.length >= 4 && t.all { it.isDigit() || it in " +-()." }
        return if (onlyNumber || t.startsWith("~")) LocalSignal.UNKNOWN_SENDER else LocalSignal.SAVED_CONTACT
    }

    fun anonymize(text: String, senderName: String? = null): AnonymizedMessage {
        val signals = linkedSetOf<LocalSignal>()
        var out = text
        out = email.replace(out, "[EMAIL]")
        out = url.replace(out) { m ->
            val host = m.groupValues[1].lowercase().removePrefix("www.")
            if (host in shorteners) signals += LocalSignal.SHORTENED_LINK
            "[LINK:$host]"
        }
        out = cpf.replace(out, "[CPF]")
        out = money.replace(out, "[VALOR]")
        out = phone.replace(out) { m -> if (m.value.count(Char::isDigit) >= 8) "[TELEFONE]" else m.value }
        out = code.replace(out, "[CODIGO]")
        out = replaceName(out, senderName)
        out = spaces.replace(out, " ").trim().take(MAX_LENGTH)
        signals += keywordSignals(text)
        return AnonymizedMessage(out, signals.toList())
    }

    private fun replaceName(text: String, senderName: String?): String {
        if (senderName.isNullOrBlank() || senderName.count(Char::isDigit) >= 4) return text
        val parts = (listOf(senderName.trim()) + senderName.split(Regex("\\s+")))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinct()
            .sortedByDescending { it.length }
        return parts.fold(text) { acc, part ->
            acc.replace(Regex("""(?iu)(?<![\p{L}\[])${Regex.escape(part)}(?![\p{L}\]])"""), "[NOME]")
        }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private val urgency = Regex("""\b(urgente|urgencia|imediat\w*|agora|hoje|ate amanha|24 ?h|bloquead\w*|suspens\w*|cancelad\w*|ultimo aviso|ultimo dia|prazo|expira\w*|rapido)\b""")
    private val pix = Regex("""\b(pix|transfer\w*|deposit\w*|boleto|pagar|pague|pagamento|taxa|emprest\w*|chave)\b""")
    private val codeRequest = Regex("""\b(senha|token)\b|\bcodigo\b.{0,40}\b(envi\w*|mand\w*|pass\w*|inform\w*|digit\w*|fal\w*|confirm\w*|repass\w*)\b|\b(envi\w*|mand\w*|pass\w*|inform\w*|repass\w*)\b.{0,40}\bcodigo\b""")

    private fun keywordSignals(text: String): List<LocalSignal> {
        val n = normalize(text)
        return buildList {
            if (urgency.containsMatchIn(n)) add(LocalSignal.URGENCY)
            if (pix.containsMatchIn(n)) add(LocalSignal.PIX_REQUEST)
            if (codeRequest.containsMatchIn(n)) add(LocalSignal.CODE_REQUEST)
        }
    }
}
