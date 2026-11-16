package br.ia.sina.data

fun scamTypeLabel(scamType: String?): String = when (scamType) {
    "FALSE_RELATIVE" -> "falso parente"
    "FALSE_BANK" -> "falsa central do banco"
    "FALSE_DELIVERY" -> "falsa entrega"
    "FALSE_PRIZE_OR_DEBT" -> "falso prêmio ou dívida"
    "PHISHING_LINK" -> "link suspeito"
    "VERIFICATION_CODE" -> "pedido de código"
    "FAKE_INVESTMENT" -> "falso investimento"
    "FALSE_BENEFIT" -> "falso benefício"
    "CLONED_ACCOUNT" -> "conta clonada"
    else -> "mensagem suspeita"
}

fun verdictTitle(verdict: Verdict): String = when (verdict) {
    Verdict.SCAM -> "Isso é golpe."
    else -> "Cuidado, pode ser golpe."
}

fun scamTypeDescription(scamType: String?): String = when (scamType) {
    "FALSE_RELATIVE" -> "Diz ser um familiar com número novo e pede dinheiro."
    "FALSE_BANK" -> "Finge ser do banco e pede dados, código ou pagamento."
    "FALSE_DELIVERY" -> "Aviso de entrega que cobra taxa ou manda abrir link."
    "FALSE_PRIZE_OR_DEBT" -> "Fala de prêmio ou dívida para pedir pagamento."
    "PHISHING_LINK" -> "Manda abrir um link de um site desconhecido."
    "VERIFICATION_CODE" -> "Pede um código que chegou no seu celular."
    "FAKE_INVESTMENT" -> "Promete lucro fácil para pedir dinheiro."
    "FALSE_BENEFIT" -> "Fala de benefício do governo para pedir dados ou pagamento."
    "CLONED_ACCOUNT" -> "Conta de alguém conhecido pedindo dinheiro."
    else -> "Mensagem com sinais de golpe."
}

fun alertListTitle(verdict: Verdict, scamType: String?): String =
    (if (verdict == Verdict.SCAM) "Golpe: " else "Cuidado: ") + scamTypeLabel(scamType)

fun formatWhen(millis: Long, now: Long = System.currentTimeMillis()): String {
    val zone = java.time.ZoneId.systemDefault()
    val date = java.time.Instant.ofEpochMilli(millis).atZone(zone)
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when (date.toLocalDate()) {
        today -> java.time.format.DateTimeFormatter.ofPattern("H:mm").format(date)
        today.minusDays(1) -> "Ontem"
        else -> java.time.format.DateTimeFormatter.ofPattern("dd/MM").format(date)
    }
}

fun formatTime(millis: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("H:mm").format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))

fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    else -> names.dropLast(1).joinToString(", ") + " e " + names.last()
}
