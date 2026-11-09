package br.ia.sina.api.device.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterRequest;
import br.ia.sina.api.device.dto.DeviceDtos.RegisterResponse;
import br.ia.sina.api.device.role.DeviceRole;
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
        if (request.role() == DeviceRole.PROTECTED && request.email() != null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "email só é aceito para GUARDIAN");
        }
        String token = tokenService.generate();
        Device device = new Device();
        device.setRole(request.role());
        device.setFcmToken(request.fcmToken());
        device.setDisplayName(request.displayName());
        device.setEmail(request.email());
        device.setTokenHash(tokenService.hash(token));
        return new RegisterResponse(repository.save(device).getId(), token);
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
        repository.save(device);
    }
}
