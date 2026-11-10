package br.ia.sina.api.notification.service;

import br.ia.sina.api.device.repository.DeviceRepository;
import br.ia.sina.api.notification.email.EmailContent;
import br.ia.sina.api.notification.email.EmailSender;
import br.ia.sina.api.notification.model.DeliveryStatus;
import br.ia.sina.api.notification.push.PushResult;
import br.ia.sina.api.notification.push.PushSender;
import br.ia.sina.api.notification.repository.AlertDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final PushSender push;
    private final EmailSender email;
    private final AlertDeliveryRepository deliveryRepository;
    private final DeviceRepository deviceRepository;

    public NotificationDispatcher(PushSender push, EmailSender email, AlertDeliveryRepository deliveryRepository,
                                  DeviceRepository deviceRepository) {
        this.push = push;
        this.email = email;
        this.deliveryRepository = deliveryRepository;
        this.deviceRepository = deviceRepository;
    }

    @Async
    public void dispatch(List<DeliveryTarget> targets, Map<String, String> pushData, EmailContent emailContent) {
        for (DeliveryTarget target : targets) {
            DeliveryStatus status = deliver(target, pushData, emailContent);
            if (target.deliveryId() != null) {
                deliveryRepository.updateStatus(target.deliveryId(), status);
            }
        }
    }

    private DeliveryStatus deliver(DeliveryTarget target, Map<String, String> pushData, EmailContent emailContent) {
        try {
            return switch (target.channel()) {
                case PUSH -> {
                    PushResult result = push.send(target.address(), pushData);
                    if (result == PushResult.INVALID_TOKEN) {
                        deviceRepository.clearFcmToken(target.guardianId(), target.address());
                    }
                    yield result == PushResult.SENT ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
                }
                case EMAIL -> {
                    email.send(target.idempotencyKey(), target.address(), emailContent);
                    yield DeliveryStatus.SENT;
                }
            };
        } catch (RuntimeException | LinkageError e) {
            log.warn("Falha no envio por {}: {}", target.channel(), e.getClass().getSimpleName());
            return DeliveryStatus.FAILED;
        }
    }
}
