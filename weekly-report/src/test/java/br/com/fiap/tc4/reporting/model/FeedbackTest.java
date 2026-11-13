package br.com.fiap.tc4.reporting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedbackTest {

    private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void acceptsRatingLimits(int rating) {
        assertDoesNotThrow(() -> new Feedback("ok", rating, Urgency.LOW, NOW));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void rejectsRatingOutOfRange(int rating) {
        assertThrows(IllegalArgumentException.class,
                () -> new Feedback("ok", rating, Urgency.LOW, NOW));
    }

    @Test
    void rejectsNullDescription() {
        assertThrows(NullPointerException.class,
                () -> new Feedback(null, 5, Urgency.MEDIUM, NOW));
    }
}