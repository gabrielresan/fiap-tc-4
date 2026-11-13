package br.com.fiap.tc4.notifications.service;

import br.com.fiap.tc4.notifications.model.CriticalNotification;

@FunctionalInterface
public interface NotificationSender {

    void send(CriticalNotification notification);
}
