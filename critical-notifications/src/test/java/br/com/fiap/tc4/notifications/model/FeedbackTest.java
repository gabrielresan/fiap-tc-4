package br.com.fiap.tc4.notifications.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    private static final Instant DATE = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void acceptsRatingBoundaries() {
        assertEquals(0, new Feedback("Feedback", 0, Urgency.CRITICAL, DATE).rating());
        assertEquals(10, new Feedback("Feedback", 10, Urgency.LOW, DATE).rating());
    }

    @Test
    void rejectsRatingsOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> new Feedback("Feedback", -1, Urgency.LOW, DATE));
        assertThrows(IllegalArgumentException.class, () -> new Feedback("Feedback", 11, Urgency.LOW, DATE));
    }

    @Test
    void rejectsMissingFields() {
        assertThrows(NullPointerException.class, () -> new Feedback(null, 5, Urgency.MEDIUM, DATE));
        assertThrows(NullPointerException.class, () -> new Feedback("Feedback", 5, null, DATE));
        assertThrows(NullPointerException.class, () -> new Feedback("Feedback", 5, Urgency.MEDIUM, null));
    }
}
