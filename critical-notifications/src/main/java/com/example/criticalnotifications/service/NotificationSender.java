package com.example.criticalnotifications.service;

import com.example.criticalnotifications.model.CriticalNotification;

@FunctionalInterface
public interface NotificationSender {

    void send(CriticalNotification notification);
}
