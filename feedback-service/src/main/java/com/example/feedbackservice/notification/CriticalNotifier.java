package com.example.feedbackservice.notification;

import com.example.feedbackservice.model.Feedback;

/**
 * Outbound port that forwards a critical feedback to the notification service.
 * Implementations throw {@link NotificationException} when the feedback could not be delivered.
 */
public interface CriticalNotifier {

    void notify(Feedback feedback);
}
