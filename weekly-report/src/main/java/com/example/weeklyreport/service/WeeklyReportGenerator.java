package com.example.weeklyreport.service;

import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportPeriod;
import com.example.weeklyreport.model.Urgency;
import com.example.weeklyreport.model.WeeklyReport;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WeeklyReportGenerator {

    public WeeklyReport generate(List<Feedback> feedbacks, ReportPeriod period) {
        Objects.requireNonNull(feedbacks, "feedbacks must not be null");
        Objects.requireNonNull(period, "period must not be null");

        feedbacks.forEach(feedback -> requireWithin(feedback, period));

        List<Feedback> sorted = feedbacks.stream()
                .sorted(Comparator.comparing(Feedback::submittedAt))
                .toList();

        return new WeeklyReport(
                period,
                averageRating(sorted),
                countByDay(sorted, period),
                countByUrgency(sorted),
                sorted);
    }

    private static void requireWithin(Feedback feedback, ReportPeriod period) {
        if (!period.contains(feedback.submittedAt())) {
            throw new IllegalArgumentException(
                    "Feedback submitted at " + feedback.submittedAt() + " is outside the report period " + period);
        }
    }

    private static double averageRating(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .mapToInt(Feedback::rating)
                .average()
                .orElse(0.0);
    }

    private static Map<LocalDate, Long> countByDay(List<Feedback> feedbacks, ReportPeriod period) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        period.days().forEach(day -> counts.put(day, 0L));
        feedbacks.forEach(feedback ->
                counts.merge(LocalDate.ofInstant(feedback.submittedAt(), ReportPeriod.ZONE), 1L, Long::sum));
        return counts;
    }

    private static Map<Urgency, Long> countByUrgency(List<Feedback> feedbacks) {
        Map<Urgency, Long> counts = new EnumMap<>(Urgency.class);
        for (Urgency urgency : Urgency.values()) {
            counts.put(urgency, 0L);
        }
        feedbacks.forEach(feedback -> counts.merge(feedback.urgency(), 1L, Long::sum));
        return counts;
    }
}