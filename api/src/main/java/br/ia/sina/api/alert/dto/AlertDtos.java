package br.ia.sina.api.alert.dto;

import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.analysis.model.Source;
import br.ia.sina.api.analysis.model.Verdict;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AlertDtos {

    private AlertDtos() {
    }

    public record AlertItem(
            UUID id,
            UUID protectedDeviceId,
            Source source,
            Verdict verdict,
            String scamType,
            double confidence,
            String familySummary,
            String messageExcerpt,
            Instant createdAt) {

        public static AlertItem of(Alert alert) {
            return new AlertItem(alert.getId(), alert.getProtectedId(), alert.getSource(), alert.getVerdict(),
                    alert.getScamType(), alert.getConfidence(), alert.getFamilySummary(), alert.getMessageExcerpt(), alert.getCreatedAt());
        }
    }

    public record AlertPage(List<AlertItem> items, String nextCursor) {
    }
}
