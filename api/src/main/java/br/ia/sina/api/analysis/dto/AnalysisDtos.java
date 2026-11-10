package br.ia.sina.api.analysis.dto;

import br.ia.sina.api.analysis.model.Engine;
import br.ia.sina.api.analysis.model.LocalSignal;
import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.analysis.model.Verdict;
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
            @NotNull Instant receivedAt) {
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
