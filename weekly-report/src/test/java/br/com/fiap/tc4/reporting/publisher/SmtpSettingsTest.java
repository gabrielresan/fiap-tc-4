package br.com.fiap.tc4.reporting.publisher;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpSettingsTest {

    private static final Map<String, String> REQUIRED = Map.of(
            "NOTIFICATION_FROM", "reports@example.com",
            "NOTIFICATION_RECIPIENTS", "admin@example.com");

    @Test
    void usesLocalMailpitDefaults() {
        SmtpSettings settings = SmtpSettings.fromEnvironment(REQUIRED);

        assertEquals("localhost", settings.host());
        assertEquals(1025, settings.port());
        assertFalse(settings.auth());
        assertFalse(settings.startTls());
    }

    @Test
    void readsAllVariables() {
        Map<String, String> env = new HashMap<>(REQUIRED);
        env.put("SMTP_HOST", "email-smtp.us-east-1.amazonaws.com");
        env.put("SMTP_PORT", "587");
        env.put("SMTP_USERNAME", "user");
        env.put("SMTP_PASSWORD", "secret");
        env.put("SMTP_AUTH", "true");
        env.put("SMTP_STARTTLS", "true");

        SmtpSettings settings = SmtpSettings.fromEnvironment(env);

        assertEquals("email-smtp.us-east-1.amazonaws.com", settings.host());
        assertEquals(587, settings.port());
        assertEquals("user", settings.username());
        assertEquals("secret", settings.password());
        assertTrue(settings.auth());
        assertTrue(settings.startTls());
    }

    @Test
    void splitsCommaSeparatedRecipients() {
        SmtpSettings settings = SmtpSettings.fromEnvironment(Map.of(
                "NOTIFICATION_FROM", "reports@example.com",
                "NOTIFICATION_RECIPIENTS", " admin@example.com, admin2@example.com ,"));

        assertEquals(List.of("admin@example.com", "admin2@example.com"), settings.recipients());
    }

    @Test
    void requiresSenderAndRecipients() {
        assertThrows(IllegalArgumentException.class,
                () -> SmtpSettings.fromEnvironment(Map.of("NOTIFICATION_RECIPIENTS", "admin@example.com")));
        assertThrows(IllegalArgumentException.class,
                () -> SmtpSettings.fromEnvironment(Map.of("NOTIFICATION_FROM", "reports@example.com")));
    }

    @Test
    void toStringDoesNotExposePassword() {
        Map<String, String> env = new HashMap<>(REQUIRED);
        env.put("SMTP_PASSWORD", "super-secret");

        assertFalse(SmtpSettings.fromEnvironment(env).toString().contains("super-secret"));
    }
}