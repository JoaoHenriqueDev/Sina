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
    private final Clock clock = new Clock() {
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    };

    @Test
    void bloqueiaNaSextaTentativaELiberaAposAJanela() {
        PairingAttemptLimiter limiter = new PairingAttemptLimiter(1000, 1000, clock);
        UUID device = UUID.randomUUID();
        for (int i = 0; i < PairingAttemptLimiter.MAX_ATTEMPTS; i++) {
            limiter.acquire(device, "1.1.1.1");
        }
        assertRateLimited(() -> limiter.acquire(device, "1.1.1.1"));
        assertThatCode(() -> limiter.acquire(UUID.randomUUID(), "1.1.1.1")).doesNotThrowAnyException();

        now = now.plus(PairingAttemptLimiter.WINDOW);
        assertThatCode(() -> limiter.acquire(device, "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void dispositivosNovosNaoRenovamOLimitePorIpNemOGlobal() {
        PairingAttemptLimiter limiter = new PairingAttemptLimiter(7, 12, clock);
        for (int i = 0; i < 7; i++) {
            limiter.acquire(UUID.randomUUID(), "2.2.2.2");
        }
        assertRateLimited(() -> limiter.acquire(UUID.randomUUID(), "2.2.2.2"));

        for (int i = 0; i < 4; i++) {
            limiter.acquire(UUID.randomUUID(), "3.3.3." + i);
        }
        assertRateLimited(() -> limiter.acquire(UUID.randomUUID(), "4.4.4.4"));
    }

    private static void assertRateLimited(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RATE_LIMITED));
    }
}
