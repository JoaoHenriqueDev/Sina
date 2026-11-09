package br.ia.sina.api.device;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class DeviceDtos {

    private DeviceDtos() {
    }

    public record RegisterRequest(
            @NotNull DeviceRole role,
            @Size(max = 4096) String fcmToken,
            @Size(max = 80) String displayName,
            @Email @Size(max = 254) String email) {
    }

    public record RegisterResponse(UUID deviceId, String deviceToken) {
    }

    public record FcmTokenRequest(@NotBlank @Size(max = 4096) String fcmToken) {
    }

    public record HeartbeatRequest(@NotNull Boolean listenerEnabled) {
    }
}
