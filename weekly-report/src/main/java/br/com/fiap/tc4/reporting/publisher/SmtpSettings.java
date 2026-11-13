package br.com.fiap.tc4.reporting.publisher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SmtpSettings(
        String host,
        int port,
        String username,
        String password,
        boolean auth,
        boolean startTls,
        String from,
        List<String> recipients) {

    public SmtpSettings {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(from, "from must not be null");
        recipients = List.copyOf(recipients);
        if (from.isBlank()) {
            throw new IllegalArgumentException("NOTIFICATION_FROM must be set");
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("NOTIFICATION_RECIPIENTS must be set");
        }
    }

    public static SmtpSettings fromEnvironment(Map<String, String> env) {
        return new SmtpSettings(
                env.getOrDefault("SMTP_HOST", "localhost"),
                Integer.parseInt(env.getOrDefault("SMTP_PORT", "1025")),
                env.getOrDefault("SMTP_USERNAME", ""),
                env.getOrDefault("SMTP_PASSWORD", ""),
                Boolean.parseBoolean(env.getOrDefault("SMTP_AUTH", "false")),
                Boolean.parseBoolean(env.getOrDefault("SMTP_STARTTLS", "false")),
                env.getOrDefault("NOTIFICATION_FROM", ""),
                splitRecipients(env.getOrDefault("NOTIFICATION_RECIPIENTS", "")));
    }

    private static List<String> splitRecipients(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(recipient -> !recipient.isEmpty())
                .toList();
    }

    @Override
    public String toString() {
        return "SmtpSettings[host=" + host + ", port=" + port + ", username=" + username
                + ", password=***, auth=" + auth + ", startTls=" + startTls
                + ", from=" + from + ", recipients=" + recipients + "]";
    }
}