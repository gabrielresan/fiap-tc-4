package br.com.fiap.tc4.feedback.service;

import br.com.fiap.tc4.feedback.model.Feedback;
import br.com.fiap.tc4.feedback.model.Urgency;
import br.com.fiap.tc4.feedback.model.UrgencyClassifier;
import br.com.fiap.tc4.feedback.notification.CriticalNotifier;
import br.com.fiap.tc4.feedback.notification.NotificationException;
import br.com.fiap.tc4.feedback.repository.FeedbackEntity;
import br.com.fiap.tc4.feedback.repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Receives a student's feedback: classifies its urgency, stores it and, when it is critical,
 * forwards it to the notification service.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository repository;
    private final UrgencyClassifier classifier;
    private final CriticalNotifier notifier;
    private final Clock clock;

    public FeedbackService(FeedbackRepository repository, UrgencyClassifier classifier,
                           CriticalNotifier notifier, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.notifier = Objects.requireNonNull(notifier, "notifier must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Feedback submit(String description, int rating) {
        Feedback feedback = new Feedback(
                UUID.randomUUID(),
                description,
                rating,
                classifier.classify(rating),
                // PostgreSQL keeps microseconds; truncating keeps the response equal to the stored value.
                clock.instant().truncatedTo(ChronoUnit.MICROS));

        repository.save(FeedbackEntity.from(feedback));
        log.info("feedback_stored id={} rating={} urgency={}", feedback.id(), feedback.rating(), feedback.urgency());

        if (feedback.urgency() == Urgency.CRITICAL) {
            notifyCritical(feedback);
        }
        return feedback;
    }

    /**
     * The feedback is already stored, so a notification failure must not reject the student's request.
     * It is logged at ERROR level so monitoring can alert on it.
     */
    private void notifyCritical(Feedback feedback) {
        try {
            notifier.notify(feedback);
            log.info("critical_notification_sent id={}", feedback.id());
        } catch (NotificationException e) {
            log.error("critical_notification_failed id={} reason={}", feedback.id(), e.getCause().getMessage(), e);
        }
    }
}
