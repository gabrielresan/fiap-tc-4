package br.com.fiap.tc4.feedback.service;

import br.com.fiap.tc4.feedback.exception.NotificationException;

import br.com.fiap.tc4.feedback.model.Feedback;

/**
 * Outbound port that forwards a critical feedback to the notification service.
 * Implementations throw {@link NotificationException} when the feedback could not be delivered.
 */
public interface CriticalNotifier {

    void notify(Feedback feedback);
}
