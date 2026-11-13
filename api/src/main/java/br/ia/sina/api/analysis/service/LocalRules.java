package br.ia.sina.api.analysis.service;

import br.ia.sina.api.analysis.model.LocalSignal;
import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Verdict;
import br.ia.sina.api.analysis.openai.LlmVerdict;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class LocalRules {

    private LocalRules() {
    }

    private static final Set<LocalSignal> SENDER_SIGNALS = EnumSet.of(LocalSignal.UNKNOWN_SENDER, LocalSignal.SAVED_CONTACT);

    static boolean hasRiskSignals(List<LocalSignal> signals) {
        return signals.stream().anyMatch(s -> !SENDER_SIGNALS.contains(s));
    }

    static LlmVerdict evaluate(List<LocalSignal> all) {
        List<LocalSignal> signals = all.stream().filter(s -> !SENDER_SIGNALS.contains(s)).distinct().toList();
        if (signals.isEmpty()) {
            // Nada foi analisado de fato: nunca devolver SAFE. Fica SUSPICIOUS com confiança baixa
            // e o AnalysisService não grava alerta para a família.
            return new LlmVerdict(Verdict.SUSPICIOUS, ScamType.NONE, 0.3,
                    "Não conseguimos analisar esta mensagem agora. Na dúvida, não clique em links, não faça Pix e não passe códigos.",
                    "Mensagem não analisada: serviço de análise indisponível e sem sinais locais.");
        }
        double confidence = Math.min(0.7, 0.4 + 0.1 * signals.size() + (all.contains(LocalSignal.UNKNOWN_SENDER) ? 0.1 : 0));
        return new LlmVerdict(Verdict.SUSPICIOUS, typeOf(signals), confidence,
                "Esta mensagem tem sinais de golpe. Não clique em links, não faça Pix e não passe códigos. Ligue para alguém de confiança no número de sempre.",
                "Mensagem com sinais de risco detectados pelas regras locais: " + signals.stream().map(Enum::name).toList() + ".");
    }

    private static ScamType typeOf(List<LocalSignal> signals) {
        if (signals.contains(LocalSignal.CODE_REQUEST)) {
            return ScamType.VERIFICATION_CODE;
        }
        if (signals.contains(LocalSignal.SHORTENED_LINK)) {
            return ScamType.PHISHING_LINK;
        }
        return ScamType.OTHER;
    }
}
