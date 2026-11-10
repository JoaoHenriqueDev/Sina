package br.ia.sina.api.notification;

import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.analysis.model.Source;

import java.time.Instant;
import java.util.UUID;

public record ScamNotification(
        UUID alertId,
        UUID protectedId,
        Source source,
        String scamType,
        double confidence,
        String familySummary,
        String messageExcerpt,
        Instant createdAt) {

    public static final int EXCERPT_MAX = 300;

    public static ScamNotification of(Alert alert, String text) {
        String excerpt = text.strip();
        if (excerpt.length() > EXCERPT_MAX) {
            excerpt = excerpt.substring(0, EXCERPT_MAX - 1) + "…";
        }
        return new ScamNotification(alert.getId(), alert.getProtectedId(), alert.getSource(), alert.getScamType(),
                alert.getConfidence(), alert.getFamilySummary(), excerpt, alert.getCreatedAt());
    }
}
