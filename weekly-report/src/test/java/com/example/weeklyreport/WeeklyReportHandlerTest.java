package com.example.weeklyreport;

import com.example.weeklyreport.publisher.SmtpReportPublisher;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.example.weeklyreport.exception.FeedbackRepositoryException;
import com.example.weeklyreport.exception.ReportPublishingException;
import com.example.weeklyreport.formatter.HtmlReportFormatter;
import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportEmail;
import com.example.weeklyreport.model.ReportPeriod;
import com.example.weeklyreport.model.Urgency;
import com.example.weeklyreport.repository.FeedbackRepository;
import com.example.weeklyreport.service.WeeklyReportGenerator;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReportHandlerTest {

    // Wednesday 2026-11-04 10:00 in Sao Paulo: the previous week is 2026-10-26..2026-11-01
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T13:00:00Z"), ZoneOffset.UTC);

    private static final List<Feedback> FEEDBACKS = List.of(
            new Feedback("Ótima aula", 9, Urgency.LOW, Instant.parse("2026-10-27T15:00:00Z")),
            new Feedback("Áudio ruim", 4, Urgency.MEDIUM, Instant.parse("2026-10-29T18:00:00Z")));

    private final List<ReportPeriod> requestedPeriods = new ArrayList<>();
    private final List<ReportEmail> sentEmails = new ArrayList<>();

    private final FeedbackRepository repository = period -> {
        requestedPeriods.add(period);
        return FEEDBACKS;
    };

    private WeeklyReportHandler handler(FeedbackRepository repository) {
        return new WeeklyReportHandler(repository, new WeeklyReportGenerator(), new HtmlReportFormatter(),
                sentEmails::add, CLOCK);
    }

    @Test
    void fetchesPreviousWeekAndSendsFormattedReport() {
        handler(repository).run();

        assertEquals(List.of(new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2))),
                requestedPeriods);
        assertEquals(1, sentEmails.size());
        assertEquals("Relatório semanal de feedbacks - 26/10/2026 a 01/11/2026", sentEmails.getFirst().subject());
        assertTrue(sentEmails.getFirst().htmlBody().contains("Total de avaliações: 2"));
        assertTrue(sentEmails.getFirst().htmlBody().contains("Média das avaliações: 6,50"));
    }

    @Test
    void returnsSummaryOfTheReport() {
        String summary = handler(repository).run();

        assertEquals("Weekly report sent: period=2026-10-26..2026-11-01, feedbacks=2, averageRating=6.50", summary);
    }

    @Test
    void handleRequestIgnoresSchedulerPayload() {
        String summary = handler(repository).handleRequest(Map.of("source", "aws.scheduler"), null);

        assertTrue(summary.startsWith("Weekly report sent"));
        assertEquals(1, sentEmails.size());
    }

    @Test
    void propagatesRepositoryFailureWithoutSending() {
        FeedbackRepository failingRepository = period -> {
            throw new FeedbackRepositoryException("database down", new SQLException("connection refused"));
        };

        assertThrows(FeedbackRepositoryException.class, () -> handler(failingRepository).run());
        assertTrue(sentEmails.isEmpty());
    }

    @Test
    void propagatesPublishingFailure() {
        WeeklyReportHandler failingHandler = new WeeklyReportHandler(repository, new WeeklyReportGenerator(),
                new HtmlReportFormatter(), email -> {
            throw new ReportPublishingException("smtp down", new RuntimeException());
        }, CLOCK);

        assertThrows(ReportPublishingException.class, failingHandler::run);
    }

    @Test
    void requiresDatabaseVariables() {
        Map<String, String> env = Map.of(
                "NOTIFICATION_FROM", "reports@example.com",
                "NOTIFICATION_RECIPIENTS", "admin@example.com");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> new WeeklyReportHandler(env, CLOCK));
        assertEquals("Environment variable DB_URL must be set", exception.getMessage());
    }

    @Test
    void usesSmtpPublisherByDefault() {
        Map<String, String> env = Map.of(
                "NOTIFICATION_FROM", "reports@example.com",
                "NOTIFICATION_RECIPIENTS", "admin@example.com");

        assertInstanceOf(SmtpReportPublisher.class, WeeklyReportHandler.publisher(env));
    }

    @Test
    void rejectsUnknownEmailProvider() {
        Map<String, String> env = Map.of(
                "NOTIFICATION_FROM", "reports@example.com",
                "NOTIFICATION_RECIPIENTS", "admin@example.com",
                "EMAIL_PROVIDER", "carrier-pigeon");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> WeeklyReportHandler.publisher(env));
        assertEquals("EMAIL_PROVIDER must be smtp or ses, got carrier-pigeon", exception.getMessage());
    }
}