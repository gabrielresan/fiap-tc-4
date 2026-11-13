package br.com.fiap.tc4.notifications.dto;

public record NotificationResponse(boolean processed, boolean logged) {
    public NotificationResponse(boolean processed) {
        this(processed, processed);
    }
}
