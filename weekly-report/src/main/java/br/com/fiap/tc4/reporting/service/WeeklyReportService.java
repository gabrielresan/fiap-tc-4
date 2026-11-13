package br.com.fiap.tc4.reporting.service;

import br.com.fiap.tc4.reporting.integration.email.ReportPublisher;
import br.com.fiap.tc4.reporting.integration.formatting.HtmlReportFormatter;
import br.com.fiap.tc4.reporting.model.ReportPeriod;
import br.com.fiap.tc4.reporting.model.WeeklyReport;
import br.com.fiap.tc4.reporting.repository.FeedbackRepository;

import java.time.Clock;
import java.util.Objects;

public final class WeeklyReportService {
    private final FeedbackRepository repository;
    private final WeeklyReportGenerator generator;
    private final HtmlReportFormatter formatter;
    private final ReportPublisher publisher;
    private final Clock clock;

    public WeeklyReportService(FeedbackRepository repository, WeeklyReportGenerator generator,
                               HtmlReportFormatter formatter, ReportPublisher publisher, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public WeeklyReport sendPreviousWeek() {
        ReportPeriod period = ReportPeriod.previousWeek(clock);
        System.out.printf("[weekly-report] Generating weekly report from %s to %s%n",
                period.startDate(), period.endDate().minusDays(1));
        WeeklyReport report = generator.generate(repository.findSubmittedWithin(period), period);
        publisher.publish(formatter.format(report));
        return report;
    }
}
