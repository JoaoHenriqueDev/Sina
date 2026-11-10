package br.ia.sina.api.notification.service;

import br.ia.sina.api.notification.model.DeliveryChannel;

import java.util.UUID;

public record DeliveryTarget(UUID deliveryId, UUID guardianId, DeliveryChannel channel, String address, String idempotencyKey) {
}
