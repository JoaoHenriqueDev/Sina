package br.ia.sina.api.analysis.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalysisRateLimiter {

    /** Por dispositivo, com folga para reanálises quando chegam mensagens novas na mesma conversa. */
    public static final int MAX_REQUESTS = 30;
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

    /** Remove janelas vencidas para o mapa não crescer com cada dispositivo que já passou por aqui. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
    public void evictExpired() {
        windows.values().removeIf(this::isExpired);
    }

    int size() {
        return windows.size();
    }

    private boolean isExpired(Window w) {
        return !clock.instant().isBefore(w.start().plus(WINDOW));
    }
}
