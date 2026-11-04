package com.example.criticalnotifications.service;

import com.example.criticalnotifications.config.NotificationProperties;
import com.example.criticalnotifications.model.CriticalNotification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SmtpNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpNotificationSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(CriticalNotification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(properties.recipients().toArray(String[]::new));
        message.setSubject("Feedback crítico recebido");
        message.setSentDate(Date.from(notification.sentAt()));
        message.setText("""
                Descrição: %s
                Urgência: %s
                Data do feedback: %s
                Data de envio: %s
                """.formatted(notification.description(), notification.urgency(),
                notification.submittedAt(), notification.sentAt()));
        mailSender.send(message);
    }
}
