package br.ia.sina.api.analysis.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalysisRateLimiter {

    public static final int MAX_REQUESTS = 20;
    public static final Duration WINDOW = Duration.ofMinutes(1);

    private record Window(Instant start, int requests) {
    }

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public AnalysisRateLimiter() {
        this(Clock.systemUTC());
    }

    AnalysisRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void acquire(UUID deviceId) {
        Window w = windows.compute(deviceId, (id, current) -> current == null || isExpired(current)
                ? new Window(clock.instant(), 1)
                : new Window(current.start(), current.requests() + 1));
        if (w.requests() > MAX_REQUESTS) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Muitas análises em pouco tempo, tente novamente em instantes");
        }
    }

    private boolean isExpired(Window w) {
        return !clock.instant().isBefore(w.start().plus(WINDOW));
    }
}
