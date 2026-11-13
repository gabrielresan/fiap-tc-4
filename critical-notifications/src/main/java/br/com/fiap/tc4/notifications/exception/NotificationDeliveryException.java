package br.com.fiap.tc4.notifications.exception;

public final class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(Throwable cause) {
        super("Failed to send critical notification via Amazon SES", cause);
    }
}
