package com.example.criticalnotifications.model;

import java.time.Instant;
import java.util.Objects;

public record CriticalNotification(
        String description,
        Urgency urgency,
        Instant submittedAt,
        Instant loggedAt
) {

    public CriticalNotification {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(urgency, "urgency must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        Objects.requireNonNull(loggedAt, "loggedAt must not be null");
        if (urgency != Urgency.CRITICAL) {
            throw new IllegalArgumentException("notification urgency must be CRITICAL");
        }
    }
}
