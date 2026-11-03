package com.example.weeklyreport.model;

import java.time.LocalDate;
import java.util.*;

public record WeeklyReport(
        ReportPeriod period,
        double averageRating,
        Map<LocalDate, Long> countByDay,
        Map<Urgency, Long> countByUrgency,
        List<Feedback> feedbacks
) {

    public WeeklyReport {
        Objects.requireNonNull(period, "period must not be null");
        countByDay = Collections.unmodifiableMap(new LinkedHashMap<>(countByDay));
        countByUrgency = Collections.unmodifiableMap(new LinkedHashMap<>(countByUrgency));
        feedbacks = List.copyOf(feedbacks);
    }

    public int totalFeedbacks() {
        return feedbacks.size();
    }
}
