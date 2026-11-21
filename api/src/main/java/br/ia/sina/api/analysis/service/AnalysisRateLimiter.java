package br.ia.sina.api.analysis.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.common.WindowLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    /** Teto diário por dispositivo para conter abuso e custo do LLM. */
    public static final int MAX_DAILY_REQUESTS = 500;
    public static final Duration DAILY_WINDOW = Duration.ofDays(1);

    private record Window(Instant start, int requests) {
    }

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
    private final Map<UUID, Window> daily = new ConcurrentHashMap<>();
    private final Clock clock;
    /** Teto global: cadastrar dispositivos é livre, então só ele limita o custo total do LLM. */
    private final WindowLimiter llmDaily;

    @Autowired
    public AnalysisRateLimiter(@Value("${sina.limits.llm-calls-per-day:5000}") int llmCallsPerDay) {
        this(Clock.systemUTC(), llmCallsPerDay);
    }

    AnalysisRateLimiter(Clock clock) {
        this(clock, Integer.MAX_VALUE);
    }

    AnalysisRateLimiter(Clock clock, int llmCallsPerDay) {
        this.clock = clock;
        this.llmDaily = new WindowLimiter(llmCallsPerDay, DAILY_WINDOW, clock);
    }

    public boolean tryAcquireLlm() {
        return llmDaily.tryAcquire("all");
    }

    public void acquire(UUID deviceId) {
        if (count(windows, deviceId, WINDOW) > MAX_REQUESTS) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Muitas análises em pouco tempo, tente novamente em instantes");
        }
        if (count(daily, deviceId, DAILY_WINDOW) > MAX_DAILY_REQUESTS) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Limite diário de análises atingido, tente novamente amanhã");
        }
    }

    private int count(Map<UUID, Window> map, UUID deviceId, Duration length) {
        return map.compute(deviceId, (id, current) -> current == null || isExpired(current, length)
                ? new Window(clock.instant(), 1)
                : new Window(current.start(), current.requests() + 1)).requests();
    }

    /** Remove janelas vencidas para o mapa não crescer com cada dispositivo que já passou por aqui. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
    public void evictExpired() {
        windows.values().removeIf(w -> isExpired(w, WINDOW));
        daily.values().removeIf(w -> isExpired(w, DAILY_WINDOW));
        llmDaily.evictExpired();
    }

    int size() {
        return windows.size();
    }

    private boolean isExpired(Window w, Duration length) {
        return !clock.instant().isBefore(w.start().plus(length));
    }
}
