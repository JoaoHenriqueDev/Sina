package br.ia.sina.api.pairing.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.common.WindowLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Limita tentativas de resgate por dispositivo, por IP e no total: como cadastrar dispositivos é livre,
 * só o limite global garante um teto de chutes contra o espaço de códigos.
 */
@Component
public class PairingAttemptLimiter {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration WINDOW = Duration.ofMinutes(10);

    private final WindowLimiter perDevice;
    private final WindowLimiter perIp;
    private final WindowLimiter global;

    @Autowired
    public PairingAttemptLimiter(@Value("${sina.limits.pairing-attempts-per-ip:20}") int perIp,
                                 @Value("${sina.limits.pairing-attempts-global:300}") int global) {
        this(perIp, global, Clock.systemUTC());
    }

    PairingAttemptLimiter(int perIp, int global, Clock clock) {
        this.perDevice = new WindowLimiter(MAX_ATTEMPTS, WINDOW, clock);
        this.perIp = new WindowLimiter(perIp, WINDOW, clock);
        this.global = new WindowLimiter(global, WINDOW, clock);
    }

    public void acquire(UUID deviceId, String clientIp) {
        boolean ok = perDevice.tryAcquire(deviceId.toString()) & perIp.tryAcquire(clientIp) & global.tryAcquire("all");
        if (!ok) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Muitas tentativas, tente novamente mais tarde");
        }
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
    public void evictExpired() {
        perDevice.evictExpired();
        perIp.evictExpired();
        global.evictExpired();
    }
}
