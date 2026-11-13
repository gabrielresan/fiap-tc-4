package br.com.fiap.tc4.reporting.dto;

import br.com.fiap.tc4.reporting.model.WeeklyReport;

import java.util.Locale;

public record WeeklyReportResponse(String periodStart, String periodEnd, int feedbackCount, double averageRating) {
    public static WeeklyReportResponse from(WeeklyReport report) {
        return new WeeklyReportResponse(report.period().startDate().toString(),
                report.period().endDate().minusDays(1).toString(), report.totalFeedbacks(), report.averageRating());
    }

    public String summary() {
        return String.format(Locale.ROOT, "Weekly report sent: period=%s..%s, feedbacks=%d, averageRating=%.2f",
                periodStart, periodEnd, feedbackCount, averageRating);
    }
}
