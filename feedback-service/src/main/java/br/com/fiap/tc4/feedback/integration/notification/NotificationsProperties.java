package br.com.fiap.tc4.feedback.integration.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Location of the critical-notifications service and the timeouts used to call it.
 */
@ConfigurationProperties("feedback.notifications")
public record NotificationsProperties(URI url, Duration connectTimeout, Duration readTimeout) {

    public NotificationsProperties {
        Objects.requireNonNull(url, "feedback.notifications.url must be set");
        Objects.requireNonNull(connectTimeout, "feedback.notifications.connect-timeout must be set");
        Objects.requireNonNull(readTimeout, "feedback.notifications.read-timeout must be set");
    }
}
