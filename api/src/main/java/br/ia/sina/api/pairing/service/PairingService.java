package br.ia.sina.api.pairing.service;

import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.pairing.dto.PairingDtos.CodeResponse;
import br.ia.sina.api.pairing.model.Pairing;
import br.ia.sina.api.pairing.model.PairingCode;
import br.ia.sina.api.pairing.repository.PairingCodeRepository;
import br.ia.sina.api.pairing.repository.PairingRepository;
import br.ia.sina.api.pairing.dto.PairingDtos.PairingItem;
import br.ia.sina.api.pairing.dto.PairingDtos.PairingList;
import br.ia.sina.api.pairing.dto.PairingDtos.PeerDevice;
import br.ia.sina.api.pairing.dto.PairingDtos.ProtectionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PairingService {

    static final Duration CODE_TTL = Duration.ofMinutes(10);
    public static final Duration INACTIVE_AFTER = Duration.ofMinutes(30);
    private static final int MAX_CODE_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final PairingCodeRepository codeRepository;
    private final PairingRepository pairingRepository;
    private final DeviceRepository deviceRepository;
    private final PairingAttemptLimiter limiter;

    public PairingService(PairingCodeRepository codeRepository, PairingRepository pairingRepository,
                          DeviceRepository deviceRepository, PairingAttemptLimiter limiter) {
        this.codeRepository = codeRepository;
        this.pairingRepository = pairingRepository;
        this.deviceRepository = deviceRepository;
        this.limiter = limiter;
    }

    @Transactional
    public CodeResponse createCode(Device protectedDevice) {
        Instant now = Instant.now();
        codeRepository.deleteExpiredOrActiveOf(protectedDevice.getId(), now);
        Instant expiresAt = now.plus(CODE_TTL);
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = "%06d".formatted(random.nextInt(1_000_000));
            if (codeRepository.insertIfFree(code, protectedDevice.getId(), expiresAt) == 1) {
                return new CodeResponse(code, expiresAt);
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de pareamento livre");
    }

    @Transactional
    public PairingItem redeem(Device guardian, String code) {
        limiter.check(guardian.getId());
        Instant now = Instant.now();
        PairingCode pairingCode = codeRepository.findByCodeAndUsedAtIsNull(code)
                .orElseThrow(() -> failure(guardian, ErrorCode.PAIRING_CODE_NOT_FOUND, "Código não encontrado"));
        if (!pairingCode.getExpiresAt().isAfter(now)) {
            throw failure(guardian, ErrorCode.PAIRING_CODE_EXPIRED, "Código expirado");
        }
        UUID protectedId = pairingCode.getProtectedId();
        if (pairingRepository.existsByProtectedIdAndGuardianId(protectedId, guardian.getId())) {
            throw new ApiException(ErrorCode.ALREADY_PAIRED, "Dispositivos já pareados");
        }
        if (codeRepository.markUsed(pairingCode.getId(), now) == 0) {
            throw failure(guardian, ErrorCode.PAIRING_CODE_NOT_FOUND, "Código não encontrado");
        }
        Pairing pairing = pairingRepository.save(new Pairing(protectedId, guardian.getId()));
        Device protectedDevice = deviceRepository.findById(protectedId).orElseThrow();
        return new PairingItem(pairing.getId(), protectedPeer(protectedDevice, now), null, pairing.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PairingList list(Device device) {
        boolean isGuardian = device.getRole() == DeviceRole.GUARDIAN;
        List<Pairing> pairings = isGuardian
                ? pairingRepository.findByGuardianIdOrderByCreatedAt(device.getId())
                : pairingRepository.findByProtectedIdOrderByCreatedAt(device.getId());
        List<UUID> peerIds = pairings.stream().map(p -> isGuardian ? p.getProtectedId() : p.getGuardianId()).toList();
        Map<UUID, Device> peers = deviceRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
        Instant now = Instant.now();
        return new PairingList(pairings.stream().map(p -> isGuardian
                ? new PairingItem(p.getId(), protectedPeer(peers.get(p.getProtectedId()), now), null, p.getCreatedAt())
                : new PairingItem(p.getId(), null, guardianPeer(peers.get(p.getGuardianId())), p.getCreatedAt())).toList());
    }

    @Transactional
    public void delete(Device device, UUID pairingId) {
        Pairing pairing = pairingRepository.findById(pairingId)
                .filter(p -> p.getProtectedId().equals(device.getId()) || p.getGuardianId().equals(device.getId()))
                .orElseThrow(() -> new ApiException(ErrorCode.PAIRING_NOT_FOUND, "Pareamento não encontrado"));
        pairingRepository.delete(pairing);
    }

    private ApiException failure(Device guardian, ErrorCode code, String message) {
        limiter.recordFailure(guardian.getId());
        return new ApiException(code, message);
    }

    private static PeerDevice protectedPeer(Device d, Instant now) {
        Instant lastSeen = d.getLastSeenAt();
        boolean active = lastSeen != null && lastSeen.isAfter(now.minus(INACTIVE_AFTER));
        return new PeerDevice(d.getId(), d.getDisplayName(), active ? ProtectionStatus.ACTIVE : ProtectionStatus.INACTIVE, lastSeen);
    }

    private static PeerDevice guardianPeer(Device d) {
        return new PeerDevice(d.getId(), d.getDisplayName(), null, null);
    }
}
