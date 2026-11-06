package com.example.criticalnotifications.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CriticalNotificationTest {

    private static final Instant DATE = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void rejectsNonCriticalUrgency() {
        for (Urgency urgency : new Urgency[]{Urgency.LOW, Urgency.MEDIUM}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CriticalNotification("Feedback", urgency, DATE, DATE));
        }
    }

    @Test
    void rejectsMissingFields() {
        assertThrows(NullPointerException.class,
                () -> new CriticalNotification(null, Urgency.CRITICAL, DATE, DATE));
        assertThrows(NullPointerException.class,
                () -> new CriticalNotification("Feedback", null, DATE, DATE));
        assertThrows(NullPointerException.class,
                () -> new CriticalNotification("Feedback", Urgency.CRITICAL, null, DATE));
        assertThrows(NullPointerException.class,
                () -> new CriticalNotification("Feedback", Urgency.CRITICAL, DATE, null));
    }
}
