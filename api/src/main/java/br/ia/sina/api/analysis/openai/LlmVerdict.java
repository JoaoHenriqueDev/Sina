package br.ia.sina.api.analysis.openai;

import br.ia.sina.api.analysis.model.ScamType;
import br.ia.sina.api.analysis.model.Verdict;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmVerdict(Verdict verdict, ScamType scamType, double confidence, String elderlyMessage, String familySummary) {

    public LlmVerdict sanitized() {
        if (verdict == null || Double.isNaN(confidence) || elderlyMessage == null || familySummary == null) {
            throw new OpenAiException("Resposta do LLM incompleta");
        }
        return new LlmVerdict(
                verdict,
                verdict == Verdict.SAFE || scamType == null ? ScamType.NONE : scamType,
                Math.clamp(confidence, 0.0, 1.0),
                truncate(elderlyMessage.strip(), 300),
                truncate(familySummary.strip(), 500));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
