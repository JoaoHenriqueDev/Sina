package br.ia.sina.api.pairing.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// shortcut: contagem em memória zera ao reiniciar e não é compartilhada entre instâncias; mover para o banco se escalar horizontalmente
@Component
public class PairingAttemptLimiter {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(10);

    private record Window(Instant start, int failures) {
    }

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public PairingAttemptLimiter() {
        this(Clock.systemUTC());
    }

    PairingAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(UUID deviceId) {
        Window w = windows.get(deviceId);
        if (w == null) {
            return;
        }
        if (isExpired(w)) {
            windows.remove(deviceId, w);
        } else if (w.failures() >= MAX_FAILURES) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Muitas tentativas, tente novamente mais tarde");
        }
    }

    public void recordFailure(UUID deviceId) {
        windows.compute(deviceId, (id, w) -> w == null || isExpired(w)
                ? new Window(clock.instant(), 1)
                : new Window(w.start(), w.failures() + 1));
    }

    private boolean isExpired(Window w) {
        return !clock.instant().isBefore(w.start().plus(WINDOW));
    }
}
