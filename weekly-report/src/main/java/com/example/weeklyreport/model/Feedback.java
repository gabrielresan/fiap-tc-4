package com.example.weeklyreport.model;

import java.time.Instant;
import java.util.Objects;

public record Feedback(
        String description,
        int rating,
        Urgency urgency,
        Instant submittedAt
) {

    public Feedback {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(urgency, "urgency must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        if (rating < 0 || rating > 10) {
            throw new IllegalArgumentException("rating must be between 0 and 10, got " + rating);
        }
    }
}
