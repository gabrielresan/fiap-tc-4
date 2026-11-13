package br.com.fiap.tc4.feedback.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rating limits used to classify feedback: ratings up to {@code criticalMax} are CRITICAL,
 * up to {@code mediumMax} are MEDIUM and anything above is LOW.
 */
@ConfigurationProperties("feedback.urgency")
public record UrgencyThresholds(int criticalMax, int mediumMax) {

    public UrgencyThresholds {
        if (criticalMax < Feedback.MIN_RATING || mediumMax > Feedback.MAX_RATING || criticalMax >= mediumMax) {
            throw new IllegalArgumentException(
                    "urgency thresholds must satisfy 0 <= critical-max < medium-max <= 10, got critical-max="
                            + criticalMax + " medium-max=" + mediumMax);
        }
    }
}
