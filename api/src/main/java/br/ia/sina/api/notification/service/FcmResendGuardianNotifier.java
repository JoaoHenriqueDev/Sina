package br.ia.sina.api.notification.service;

import br.ia.sina.api.device.model.Device;
import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.notification.GuardianNotifier;
import br.ia.sina.api.notification.ScamNotification;
import br.ia.sina.api.notification.email.EmailSender;
import br.ia.sina.api.notification.model.AlertDelivery;
import br.ia.sina.api.notification.model.DeliveryChannel;
import br.ia.sina.api.notification.push.PushSender;
import br.ia.sina.api.notification.repository.AlertDeliveryRepository;
import br.ia.sina.api.pairing.model.Pairing;
import br.ia.sina.api.pairing.repository.PairingRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FcmResendGuardianNotifier implements GuardianNotifier {

    private record Channel(Device guardian, DeliveryChannel channel, String address) {
    }

    private final PairingRepository pairingRepository;
    private final DeviceRepository deviceRepository;
    private final AlertDeliveryRepository deliveryRepository;
    private final PushSender push;
    private final EmailSender email;
    private final NotificationDispatcher dispatcher;

    public FcmResendGuardianNotifier(PairingRepository pairingRepository, DeviceRepository deviceRepository,
                                     AlertDeliveryRepository deliveryRepository, PushSender push, EmailSender email,
                                     NotificationDispatcher dispatcher) {
        this.pairingRepository = pairingRepository;
        this.deviceRepository = deviceRepository;
        this.deliveryRepository = deliveryRepository;
        this.push = push;
        this.email = email;
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean notifyScam(ScamNotification n) {
        List<Channel> channels = channelsOf(n.protectedId());
        if (channels.isEmpty()) {
            return false;
        }
        List<DeliveryTarget> targets = new ArrayList<>();
        for (Channel c : channels) {
            AlertDelivery delivery = deliveryRepository.save(new AlertDelivery(n.alertId(), c.guardian().getId(), c.channel()));
            targets.add(new DeliveryTarget(delivery.getId(), c.guardian().getId(), c.channel(), c.address(),
                    "alert-" + delivery.getId()));
        }
        String name = protectedName(n.protectedId());
        dispatcher.dispatch(targets, NotificationMessages.scamPush(n, name), NotificationMessages.scamEmail(n, name));
        return true;
    }

    @Override
    public void notifyInactive(Device protectedDevice) {
        List<Channel> channels = channelsOf(protectedDevice.getId());
        if (channels.isEmpty()) {
            return;
        }
        String key = "inactive-" + protectedDevice.getId() + "-" + protectedDevice.getLastSeenAt().toEpochMilli();
        List<DeliveryTarget> targets = channels.stream()
                .map(c -> new DeliveryTarget(null, c.guardian().getId(), c.channel(), c.address(),
                        key + "-" + c.guardian().getId()))
                .toList();
        String name = protectedDevice.getDisplayName();
        dispatcher.dispatch(targets,
                NotificationMessages.inactivePush(protectedDevice.getId(), name, protectedDevice.getLastSeenAt()),
                NotificationMessages.inactiveEmail(name, protectedDevice.getLastSeenAt()));
    }

    private List<Channel> channelsOf(UUID protectedId) {
        List<UUID> guardianIds = pairingRepository.findByProtectedIdOrderByCreatedAt(protectedId).stream()
                .map(Pairing::getGuardianId)
                .toList();
        if (guardianIds.isEmpty()) {
            return List.of();
        }
        boolean pushOn = push.enabled();
        boolean emailOn = email.enabled();
        List<Channel> channels = new ArrayList<>();
        for (Device guardian : deviceRepository.findAllById(guardianIds)) {
            if (pushOn && hasText(guardian.getFcmToken())) {
                channels.add(new Channel(guardian, DeliveryChannel.PUSH, guardian.getFcmToken()));
            }
            if (emailOn && hasText(guardian.getEmail())) {
                channels.add(new Channel(guardian, DeliveryChannel.EMAIL, guardian.getEmail()));
            }
        }
        return channels;
    }

    private String protectedName(UUID protectedId) {
        return deviceRepository.findById(protectedId).map(Device::getDisplayName).orElse(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
