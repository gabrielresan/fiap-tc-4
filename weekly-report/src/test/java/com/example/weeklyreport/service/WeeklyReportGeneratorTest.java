package com.example.weeklyreport.service;

import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportPeriod;
import com.example.weeklyreport.model.Urgency;
import com.example.weeklyreport.model.WeeklyReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReportGeneratorTest {

    private static final ReportPeriod PERIOD =
            new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

    private final WeeklyReportGenerator generator = new WeeklyReportGenerator();

    private static Feedback feedback(int rating, Urgency urgency, String submittedAt) {
        return new Feedback("feedback " + rating, rating, urgency, Instant.parse(submittedAt));
    }

    @Test
    void emptyWeekHasZeroAverageAndZeroCounts() {
        WeeklyReport report = generator.generate(List.of(), PERIOD);

        assertEquals(0, report.totalFeedbacks());
        assertEquals(0.0, report.averageRating());
        assertEquals(7, report.countByDay().size());
        assertTrue(report.countByDay().values().stream().allMatch(count -> count == 0L));
        assertEquals(Map.of(Urgency.LOW, 0L, Urgency.MEDIUM, 0L, Urgency.CRITICAL, 0L),
                report.countByUrgency());
    }

    @Test
    void calculatesAverageRating() {
        List<Feedback> feedbacks = List.of(
                feedback(2, Urgency.CRITICAL, "2026-10-26T15:00:00Z"),
                feedback(7, Urgency.LOW, "2026-10-27T15:00:00Z"),
                feedback(8, Urgency.LOW, "2026-10-28T15:00:00Z"));

        WeeklyReport report = generator.generate(feedbacks, PERIOD);

        assertEquals(17.0 / 3, report.averageRating(), 0.0001);
    }

    @Test
    void countsFeedbacksPerDayIncludingDaysWithoutFeedback() {
        List<Feedback> feedbacks = List.of(
                feedback(5, Urgency.MEDIUM, "2026-10-26T15:00:00Z"),
                feedback(6, Urgency.MEDIUM, "2026-10-26T18:00:00Z"),
                feedback(9, Urgency.LOW, "2026-10-30T12:00:00Z"));

        WeeklyReport report = generator.generate(feedbacks, PERIOD);

        Map<LocalDate, Long> byDay = report.countByDay();
        assertEquals(PERIOD.days(), List.copyOf(byDay.keySet()));
        assertEquals(2L, byDay.get(LocalDate.of(2026, 10, 26)));
        assertEquals(0L, byDay.get(LocalDate.of(2026, 10, 27)));
        assertEquals(1L, byDay.get(LocalDate.of(2026, 10, 30)));
    }

    @Test
    void groupsDaysUsingSaoPauloTimeZone() {
        // 2026-10-28 01:30 UTC is still 2026-10-27 22:30 in Sao Paulo
        WeeklyReport report = generator.generate(
                List.of(feedback(4, Urgency.MEDIUM, "2026-10-28T01:30:00Z")), PERIOD);

        assertEquals(1L, report.countByDay().get(LocalDate.of(2026, 10, 27)));
        assertEquals(0L, report.countByDay().get(LocalDate.of(2026, 10, 28)));
    }

    @Test
    void countsFeedbacksPerUrgencyInEnumOrder() {
        List<Feedback> feedbacks = List.of(
                feedback(1, Urgency.CRITICAL, "2026-10-26T15:00:00Z"),
                feedback(0, Urgency.CRITICAL, "2026-10-27T15:00:00Z"),
                feedback(10, Urgency.LOW, "2026-10-28T15:00:00Z"));

        WeeklyReport report = generator.generate(feedbacks, PERIOD);

        assertEquals(List.of(Urgency.LOW, Urgency.MEDIUM, Urgency.CRITICAL),
                List.copyOf(report.countByUrgency().keySet()));
        assertEquals(1L, report.countByUrgency().get(Urgency.LOW));
        assertEquals(0L, report.countByUrgency().get(Urgency.MEDIUM));
        assertEquals(2L, report.countByUrgency().get(Urgency.CRITICAL));
    }

    @Test
    void sortsFeedbacksBySubmissionDate() {
        Feedback later = feedback(3, Urgency.CRITICAL, "2026-10-30T15:00:00Z");
        Feedback earlier = feedback(8, Urgency.LOW, "2026-10-26T15:00:00Z");

        WeeklyReport report = generator.generate(List.of(later, earlier), PERIOD);

        assertEquals(List.of(earlier, later), report.feedbacks());
    }

    @Test
    void rejectsFeedbackOutsidePeriod() {
        // 2026-11-02 00:00 in Sao Paulo is the exclusive end of the period
        List<Feedback> feedbacks = List.of(feedback(5, Urgency.MEDIUM, "2026-11-02T03:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> generator.generate(feedbacks, PERIOD));
    }
}