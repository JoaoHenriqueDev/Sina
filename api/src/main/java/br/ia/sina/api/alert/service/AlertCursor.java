package br.ia.sina.api.alert.service;

import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

record AlertCursor(Instant createdAt, UUID id) {

    static AlertCursor of(Alert alert) {
        return new AlertCursor(alert.getCreatedAt(), alert.getId());
    }

    String encode() {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static AlertCursor decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 2) {
                throw invalid();
            }
            return new AlertCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.VALIDATION_ERROR, "cursor: valor inválido");
    }
}
