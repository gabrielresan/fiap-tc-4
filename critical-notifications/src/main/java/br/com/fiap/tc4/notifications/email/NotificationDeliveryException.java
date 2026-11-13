package br.com.fiap.tc4.notifications.email;

public final class NotificationDeliveryException extends RuntimeException {
    NotificationDeliveryException(Throwable cause) {
        super("Failed to send critical notification via Amazon SES", cause);
    }
}
