package com.example.feedbackservice.api;

import com.example.feedbackservice.model.Feedback;
import com.example.feedbackservice.model.Urgency;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        String descricao,
        int nota,
        Urgency urgencia,
        Instant dataEnvio
) {

    static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.id(),
                feedback.description(),
                feedback.rating(),
                feedback.urgency(),
                feedback.submittedAt());
    }
}
