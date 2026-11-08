package com.example.feedbackservice.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Feedback(
        UUID id,
        String description,
        int rating,
        Urgency urgency,
        Instant submittedAt
) {

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;

    public Feedback {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(urgency, "urgency must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("rating must be between 0 and 10, got " + rating);
        }
    }
}
