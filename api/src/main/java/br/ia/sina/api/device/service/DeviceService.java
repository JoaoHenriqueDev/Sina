package br.ia.sina.api.device.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterRequest;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterResponse;
import br.ia.sina.api.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final TokenService tokenService;

    public DeviceService(DeviceRepository repository, TokenService tokenService) {
        this.repository = repository;
        this.tokenService = tokenService;
    }

    public RegisterResponse register(RegisterRequest request) {
        String phone = normalizePhone(request.phone());
        String token = tokenService.generate();
        Device device = new Device();
        device.setRole(request.role());
        device.setFcmToken(request.fcmToken());
        device.setDisplayName(request.displayName().strip());
        device.setEmail(request.email().strip());
        device.setPhone(phone);
        device.setTokenHash(tokenService.hash(token));
        return new RegisterResponse(repository.save(device).getId(), token);
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

    public void heartbeat(Device device, boolean listenerEnabled) {
        if (!listenerEnabled) {
            return;
        }
        device.setLastSeenAt(Instant.now());
        device.setInactiveNotifiedAt(null);
        repository.save(device);
    }
}
