package br.com.fiap.tc4.feedback.model;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Derives the urgency of a feedback from its rating. This service is the only one that classifies
 * urgency; the notification and report services trust the stored value.
 */
@Component
public class UrgencyClassifier {

    private final UrgencyThresholds thresholds;

    public UrgencyClassifier(UrgencyThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds must not be null");
    }

    public Urgency classify(int rating) {
        if (rating < Feedback.MIN_RATING || rating > Feedback.MAX_RATING) {
            throw new IllegalArgumentException("rating must be between 0 and 10, got " + rating);
        }
        if (rating <= thresholds.criticalMax()) {
            return Urgency.CRITICAL;
        }
        if (rating <= thresholds.mediumMax()) {
            return Urgency.MEDIUM;
        }
        return Urgency.LOW;
    }
}
