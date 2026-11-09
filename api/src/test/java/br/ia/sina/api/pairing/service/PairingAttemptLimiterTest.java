package br.ia.sina.api.pairing.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PairingAttemptLimiterTest {

    private Instant now = Instant.parse("2026-11-09T12:00:00Z");
    private final PairingAttemptLimiter limiter = new PairingAttemptLimiter(new Clock() {
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    });
    private final UUID device = UUID.randomUUID();

    @Test
    void bloqueiaNaSextaTentativaELiberaAposAJanela() {
        for (int i = 0; i < PairingAttemptLimiter.MAX_FAILURES; i++) {
            limiter.check(device);
            limiter.recordFailure(device);
        }
        assertThatThrownBy(() -> limiter.check(device))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RATE_LIMITED));
        assertThatCode(() -> limiter.check(UUID.randomUUID())).doesNotThrowAnyException();

        now = now.plus(PairingAttemptLimiter.WINDOW);
        assertThatCode(() -> limiter.check(device)).doesNotThrowAnyException();
    }
}
