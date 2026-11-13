package br.com.fiap.tc4.notifications.service;

import br.com.fiap.tc4.notifications.model.CriticalNotification;
import br.com.fiap.tc4.notifications.model.Feedback;
import br.com.fiap.tc4.notifications.model.Urgency;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CriticalNotificationServiceTest {

    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant SENT_AT = Instant.parse("2026-11-03T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(SENT_AT, ZoneOffset.UTC);

    private final List<CriticalNotification> sent = new ArrayList<>();
    private final CriticalNotificationService service = new CriticalNotificationService(sent::add, CLOCK);

    @Test
    void sendsCriticalFeedbackWithDescriptionUrgencyAndDates() {
        Feedback feedback = new Feedback("Aula indisponível", 1, Urgency.CRITICAL, SUBMITTED_AT);

        assertTrue(service.notify(feedback));

        assertEquals(List.of(new CriticalNotification(
                "Aula indisponível", Urgency.CRITICAL, SUBMITTED_AT, SENT_AT)), sent);
    }

    @Test
    void ignoresLowAndMediumUrgency() {
        for (Urgency urgency : List.of(Urgency.LOW, Urgency.MEDIUM)) {
            assertFalse(service.notify(new Feedback("Avaliação", 0, urgency, SUBMITTED_AT)));
        }

        assertTrue(sent.isEmpty());
    }

    @Test
    void usesExplicitUrgencyRegardlessOfRating() {
        assertTrue(service.notify(new Feedback("Problema crítico", 10, Urgency.CRITICAL, SUBMITTED_AT)));

        assertEquals(1, sent.size());
    }

    @Test
    void propagatesSenderFailure() {
        IllegalStateException failure = new IllegalStateException("Delivery failed");
        CriticalNotificationService failingService = new CriticalNotificationService(notification -> {
            throw failure;
        }, CLOCK);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> failingService.notify(
                new Feedback("Problema crítico", 1, Urgency.CRITICAL, SUBMITTED_AT))));
    }

    @Test
    void rejectsNullFeedbackWithoutSending() {
        assertThrows(NullPointerException.class, () -> service.notify(null));

        assertTrue(sent.isEmpty());
    }

    @Test
    void rejectsMissingDependencies() {
        assertThrows(NullPointerException.class, () -> new CriticalNotificationService(null, CLOCK));
        assertThrows(NullPointerException.class, () -> new CriticalNotificationService(sent::add, null));
    }
}
