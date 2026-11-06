package com.example.criticalnotifications.service;

import com.example.criticalnotifications.model.CriticalNotification;
import com.example.criticalnotifications.model.Urgency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class LoggingNotificationSenderTest {

    private final LoggingNotificationSender sender = new LoggingNotificationSender();

    @Test
    void logsRequiredNotificationData(CapturedOutput output) {
        sender.send(new CriticalNotification("Aula indisponível", Urgency.CRITICAL,
                Instant.parse("2026-11-06T12:00:00Z"), Instant.parse("2026-11-06T12:01:00Z")));

        assertTrue(output.getOut().contains("critical_notification description=Aula indisponível"));
        assertTrue(output.getOut().contains("urgency=CRITICAL"));
        assertTrue(output.getOut().contains("submittedAt=2026-11-06T12:00:00Z"));
        assertTrue(output.getOut().contains("loggedAt=2026-11-06T12:01:00Z"));
    }

    @Test
    void escapesLineBreaksInDescription(CapturedOutput output) {
        sender.send(new CriticalNotification("Aula\r\nindisponível\tagora", Urgency.CRITICAL,
                Instant.EPOCH, Instant.EPOCH));

        assertTrue(output.getOut().contains("description=Aula\\r\\nindisponível\\tagora"));
        assertFalse(output.getOut().contains("Aula\r\nindisponível"));
    }

    @Test
    void rejectsNullNotification() {
        assertThrows(NullPointerException.class, () -> sender.send(null));
    }
}
