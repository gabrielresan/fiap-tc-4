package br.com.fiap.tc4.notifications.service;

import br.com.fiap.tc4.notifications.model.CriticalNotification;
import br.com.fiap.tc4.notifications.model.Feedback;
import br.com.fiap.tc4.notifications.model.Urgency;

import java.time.Clock;
import java.util.Objects;

public class CriticalNotificationService {

    private final NotificationSender sender;
    private final Clock clock;

    public CriticalNotificationService(NotificationSender sender, Clock clock) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public boolean notify(Feedback feedback) {
        Objects.requireNonNull(feedback, "feedback must not be null");
        if (feedback.urgency() != Urgency.CRITICAL) {
            return false;
        }

        sender.send(new CriticalNotification(
                feedback.description(),
                feedback.urgency(),
                feedback.submittedAt(),
                clock.instant()));
        return true;
    }
}
