package br.ia.sina.api.alert.service;

import br.ia.sina.api.alert.dto.AlertDtos.AlertItem;
import br.ia.sina.api.alert.dto.AlertDtos.AlertPage;
import br.ia.sina.api.alert.model.Alert;
import br.ia.sina.api.alert.repository.AlertRepository;
import br.ia.sina.api.common.ApiException;
import br.ia.sina.api.common.ErrorCode;
import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.pairing.model.Pairing;
import br.ia.sina.api.pairing.repository.PairingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final PairingRepository pairingRepository;

    public AlertService(AlertRepository alertRepository, PairingRepository pairingRepository) {
        this.alertRepository = alertRepository;
        this.pairingRepository = pairingRepository;
    }

    @Transactional(readOnly = true)
    public AlertPage list(Device guardian, UUID protectedId, int limit, String cursor) {
        AlertCursor after = cursor == null || cursor.isBlank() ? null : AlertCursor.decode(cursor);
        Set<UUID> protectedIds = pairingRepository.findByGuardianIdOrderByCreatedAt(guardian.getId()).stream()
                .map(Pairing::getProtectedId)
                .collect(Collectors.toSet());
        if (protectedId != null) {
            if (!protectedIds.contains(protectedId)) {
                throw new ApiException(ErrorCode.PAIRING_NOT_FOUND, "Pareamento não encontrado");
            }
            protectedIds = Set.of(protectedId);
        }
        if (protectedIds.isEmpty()) {
            return new AlertPage(List.of(), null);
        }
        List<Alert> rows = after == null
                ? alertRepository.findFirstPage(protectedIds, limit + 1)
                : alertRepository.findPageAfter(protectedIds, after.createdAt(), after.id(), limit + 1);
        boolean hasMore = rows.size() > limit;
        List<Alert> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? AlertCursor.of(page.getLast()).encode() : null;
        return new AlertPage(page.stream().map(AlertItem::of).toList(), nextCursor);
    }
}
