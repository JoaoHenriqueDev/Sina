package br.ia.sina.api.notification;

import org.springframework.stereotype.Component;

@Component
public class NoopGuardianNotifier implements GuardianNotifier {

    @Override
    public boolean notifyScam(ScamNotification notification) {
        return false;
    }
}
