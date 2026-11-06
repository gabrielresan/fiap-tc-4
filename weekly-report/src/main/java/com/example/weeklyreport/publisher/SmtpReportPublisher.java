package com.example.weeklyreport.publisher;

import com.example.weeklyreport.exception.ReportPublishingException;
import com.example.weeklyreport.model.ReportEmail;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class SmtpReportPublisher implements ReportPublisher {

    private static final String TIMEOUT_MILLIS = "5000";

    private final SmtpSettings settings;
    private final Session session;
    private final InternetAddress from;
    private final InternetAddress[] recipients;

    public SmtpReportPublisher(SmtpSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.session = Session.getInstance(mailProperties(settings));
        try {
            this.from = new InternetAddress(settings.from(), true);
            this.recipients = InternetAddress.parse(String.join(",", settings.recipients()), true);
        } catch (AddressException e) {
            throw new IllegalArgumentException("Invalid e-mail address in SMTP settings", e);
        }
    }

    @Override
    public void publish(ReportEmail email) {
        Objects.requireNonNull(email, "email must not be null");
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(from);
            message.setRecipients(Message.RecipientType.TO, recipients);
            message.setSubject(email.subject(), StandardCharsets.UTF_8.name());
            message.setText(email.htmlBody(), StandardCharsets.UTF_8.name(), "html");
            message.setSentDate(new Date());

            if (settings.auth()) {
                Transport.send(message, settings.username(), settings.password());
            } else {
                Transport.send(message);
            }
        } catch (MessagingException e) {
            throw new ReportPublishingException(
                    "Failed to send weekly report e-mail via " + settings.host() + ":" + settings.port(), e);
        }
    }

    private static Properties mailProperties(SmtpSettings settings) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", settings.host());
        properties.put("mail.smtp.port", String.valueOf(settings.port()));
        properties.put("mail.smtp.auth", String.valueOf(settings.auth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(settings.startTls()));
        properties.put("mail.smtp.starttls.required", String.valueOf(settings.startTls()));
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
        properties.put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS);
        properties.put("mail.smtp.timeout", TIMEOUT_MILLIS);
        properties.put("mail.smtp.writetimeout", TIMEOUT_MILLIS);
        return properties;
    }
}