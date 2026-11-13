package br.ia.sina.api.pairing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PairingDtos {

    private PairingDtos() {
    }

    public enum ProtectionStatus {
        ACTIVE,
        INACTIVE
    }

    public record CodeResponse(String code, Instant expiresAt) {
    }

    public record RedeemRequest(@NotNull @Pattern(regexp = "\\d{6}") String code) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PeerDevice(UUID deviceId, String displayName, ProtectionStatus protectionStatus, Instant lastSeenAt) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PairingItem(UUID pairingId,
                              @JsonProperty("protected") PeerDevice protectedDevice,
                              PeerDevice guardian,
                              Instant createdAt) {
    }

    public record PairingList(List<PairingItem> items) {
    }
}
