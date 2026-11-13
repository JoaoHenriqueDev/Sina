package br.ia.sina.api.analysis.dto;

import br.ia.sina.api.analysis.model.Engine;
import br.ia.sina.api.analysis.model.LocalSignal;
import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.analysis.model.Verdict;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    public record AnalyzeRequest(
            @NotNull Source source,
            @NotBlank @Size(max = 1000) String text,
            @Size(max = 10) List<@NotNull LocalSignal> localSignals,
            @NotNull Instant receivedAt,
            @Min(1) @Max(50) Integer messageCount,
            @Size(max = 1000) String context) {

        public AnalyzeRequest(Source source, String text, List<LocalSignal> localSignals, Instant receivedAt) {
            this(source, text, localSignals, receivedAt, null, null);
        }

        public String contextOrNull() {
            return context == null || context.isBlank() ? null : context.strip();
        }
    }

    public record AnalyzeResponse(
            Verdict verdict,
            String scamType,
            double confidence,
            String elderlyMessage,
            Engine engine,
            UUID alertId,
            boolean guardianNotified) {
    }
}
