package br.ia.sina.api.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// shortcut: contagem em memória zera ao reiniciar e não é compartilhada entre instâncias; mover para o banco se escalar horizontalmente
public final class WindowLimiter {

    private record Window(Instant start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int max;
    private final Duration length;
    private final Clock clock;

    public WindowLimiter(int max, Duration length, Clock clock) {
        this.max = max;
        this.length = length;
        this.clock = clock;
    }

    /** Conta a tentativa de forma atômica e diz se ela ainda cabe na janela. */
    public boolean tryAcquire(String key) {
        return windows.compute(key, (k, w) -> w == null || isExpired(w)
                ? new Window(clock.instant(), 1)
                : new Window(w.start(), w.count() + 1)).count() <= max;
    }

    public void evictExpired() {
        windows.values().removeIf(this::isExpired);
    }

    public int size() {
        return windows.size();
    }

    private boolean isExpired(Window w) {
        return !clock.instant().isBefore(w.start().plus(length));
    }
}
