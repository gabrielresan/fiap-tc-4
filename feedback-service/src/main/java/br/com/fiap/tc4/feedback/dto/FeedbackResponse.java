package br.com.fiap.tc4.feedback.dto;

import br.com.fiap.tc4.feedback.model.Feedback;
import br.com.fiap.tc4.feedback.model.Urgency;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        String descricao,
        int nota,
        Urgency urgencia,
        Instant dataEnvio
) {

    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.id(),
                feedback.description(),
                feedback.rating(),
                feedback.urgency(),
                feedback.submittedAt());
    }
}
