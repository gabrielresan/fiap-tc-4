package br.com.fiap.tc4.notifications.logging;

import br.com.fiap.tc4.notifications.model.CriticalNotification;
import br.com.fiap.tc4.notifications.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(CriticalNotification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        LOGGER.info("critical_notification description={} urgency={} submittedAt={} loggedAt={}",
                notification.description().replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"),
                notification.urgency(), notification.submittedAt(), notification.loggedAt());
    }
}
