package br.ia.sina.api.analysis.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisRateLimiterTest {

    @Test
    void bloqueiaAcimaDoLimiteELiberaNaProximaJanela() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-10T12:00:00Z");
        when(clock.instant()).thenReturn(start);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        AnalysisRateLimiter limiter = new AnalysisRateLimiter(clock);
        UUID id = UUID.randomUUID();

        for (int i = 0; i < AnalysisRateLimiter.MAX_REQUESTS; i++) {
            limiter.acquire(id);
        }
        assertThatThrownBy(() -> limiter.acquire(id))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RATE_LIMITED));

        when(clock.instant()).thenReturn(start.plus(AnalysisRateLimiter.WINDOW));
        limiter.acquire(id);
    }
}
