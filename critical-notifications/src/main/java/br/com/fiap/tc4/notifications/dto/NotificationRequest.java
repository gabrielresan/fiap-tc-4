package br.com.fiap.tc4.notifications.dto;

import br.com.fiap.tc4.notifications.model.Feedback;
import br.com.fiap.tc4.notifications.model.Urgency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record NotificationRequest(
        @NotBlank String description,
        @NotNull @Min(0) @Max(10) Integer rating,
        @NotNull Urgency urgency,
        @NotNull Instant submittedAt
) {

    public Feedback toFeedback() {
        return new Feedback(description, rating, urgency, submittedAt);
    }
}
