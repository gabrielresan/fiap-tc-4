package com.example.criticalnotifications.service;

import com.example.criticalnotifications.config.NotificationProperties;
import com.example.criticalnotifications.model.CriticalNotification;
import com.example.criticalnotifications.model.Urgency;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpNotificationSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpNotificationSender sender = new SmtpNotificationSender(mailSender,
            new NotificationProperties("notifications@example.com", List.of("admin@example.com", "admin2@example.com")));
    private final CriticalNotification notification = new CriticalNotification("Aula indisponível", Urgency.CRITICAL,
            Instant.parse("2026-11-04T12:00:00Z"), Instant.parse("2026-11-04T12:01:00Z"));

    @Test
    void sendsEmailToConfiguredAdministratorsWithRequiredData() {
        sender.send(notification);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("notifications@example.com", message.getFrom());
        assertArrayEquals(new String[]{"admin@example.com", "admin2@example.com"}, message.getTo());
        assertEquals("Feedback crítico recebido", message.getSubject());
        assertEquals(Date.from(notification.sentAt()), message.getSentDate());
        assertEquals("""
                Descrição: Aula indisponível
                Urgência: CRITICAL
                Data do feedback: 2026-11-04T12:00:00Z
                Data de envio: 2026-11-04T12:01:00Z
                """, message.getText());
    }

    @Test
    void propagatesSmtpFailure() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

        assertSame(failure, assertThrows(MailSendException.class, () -> sender.send(notification)));
    }
}
