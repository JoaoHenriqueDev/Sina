package br.ia.sina.api.notification;

import br.ia.sina.api.device.model.Device;

import java.util.Optional;
import java.util.UUID;

public interface GuardianNotifier {

    /**
     * Grava as entregas do alerta (uma por canal de cada familiar) e devolve o envio a ser disparado
     * depois que a transação de quem chamou for confirmada. Vazio quando não há familiar com canal ativo.
     */
    Optional<Runnable> prepareScam(ScamNotification notification);

    /** Indica se o alerta já tem entregas gravadas, ou seja, se a família já foi acionada para ele. */
    boolean alreadyNotified(UUID alertId);

    void notifyInactive(Device protectedDevice);

    default boolean notifyScam(ScamNotification notification) {
        Optional<Runnable> send = prepareScam(notification);
        send.ifPresent(Runnable::run);
        return send.isPresent();
    }
}
