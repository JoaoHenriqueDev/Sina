package br.ia.sina.api.device.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterRequest;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterResponse;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.common.WindowLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerification;
    private final WindowLimiter registrations;

    public DeviceService(DeviceRepository repository, TokenService tokenService, EmailVerificationService emailVerification,
                         @Value("${sina.limits.registrations-per-hour-per-ip:30}") int registrationsPerHour) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.emailVerification = emailVerification;
        this.registrations = new WindowLimiter(registrationsPerHour, Duration.ofHours(1), Clock.systemUTC());
    }

    public RegisterResponse register(RegisterRequest request, String clientIp) {
        if (!registrations.tryAcquire(clientIp)) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Muitos cadastros a partir desta rede, tente novamente mais tarde");
        }
        String phone = normalizePhone(request.phone());
        String token = tokenService.generate();
        Device device = new Device();
        device.setRole(request.role());
        device.setFcmToken(request.fcmToken());
        device.setDisplayName(request.displayName().strip());
        device.setEmail(request.email().strip());
        device.setPhone(phone);
        device.setTokenHash(tokenService.hash(token));
        String emailToken = request.role() == DeviceRole.GUARDIAN ? emailVerification.prepare(device) : null;
        Device saved = repository.save(device);
        if (emailToken != null) {
            emailVerification.sendLink(saved.getId().toString(), saved.getEmail(), emailToken);
        }
        return new RegisterResponse(saved.getId(), token);
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    public void evictExpiredRegistrations() {
        registrations.evictExpired();
    }

    static String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 13) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "telefone deve ter DDD e número");
        }
        return digits;
    }

    public void updateFcmToken(Device device, String fcmToken) {
        device.setFcmToken(fcmToken);
        repository.save(device);
    }

    public void delete(Device device) {
        repository.deleteById(device.getId());
    }

    public void heartbeat(Device device, boolean listenerEnabled) {
        if (!listenerEnabled) {
            return;
        }
        device.setLastSeenAt(Instant.now());
        device.setInactiveNotifiedAt(null);
        repository.save(device);
    }
}
