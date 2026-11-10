package br.ia.sina.api.notification;

import br.ia.sina.api.device.model.Device;

public interface GuardianNotifier {

    boolean notifyScam(ScamNotification notification);

    void notifyInactive(Device protectedDevice);
}
