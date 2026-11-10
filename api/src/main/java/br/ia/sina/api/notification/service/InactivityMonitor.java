package br.ia.sina.api.notification.service;

import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.device.role.DeviceRole;
import br.ia.sina.api.notification.GuardianNotifier;
import br.ia.sina.api.pairing.service.PairingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InactivityMonitor {

    private static final Logger log = LoggerFactory.getLogger(InactivityMonitor.class);

    private final DeviceRepository deviceRepository;
    private final GuardianNotifier notifier;

    public InactivityMonitor(DeviceRepository deviceRepository, GuardianNotifier notifier) {
        this.deviceRepository = deviceRepository;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void run() {
        check(Instant.now());
    }

    public int check(Instant now) {
        Instant limit = now.minus(PairingService.INACTIVE_AFTER);
        int notified = 0;
        for (Device device : deviceRepository.findByRoleAndLastSeenAtBeforeAndInactiveNotifiedAtIsNull(DeviceRole.PROTECTED, limit)) {
            if (deviceRepository.markInactiveNotified(device.getId(), limit, now) != 1) {
                continue;
            }
            try {
                notifier.notifyInactive(device);
                notified++;
            } catch (RuntimeException e) {
                log.warn("Falha ao avisar inatividade: {}", e.getClass().getSimpleName());
            }
        }
        return notified;
    }
}
