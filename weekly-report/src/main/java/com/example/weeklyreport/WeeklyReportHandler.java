package com.example.weeklyreport;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.weeklyreport.formatter.HtmlReportFormatter;
import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportPeriod;
import com.example.weeklyreport.model.WeeklyReport;
import com.example.weeklyreport.publisher.ReportPublisher;
import com.example.weeklyreport.publisher.SmtpReportPublisher;
import com.example.weeklyreport.publisher.SmtpSettings;
import com.example.weeklyreport.repository.FeedbackRepository;
import com.example.weeklyreport.repository.JdbcFeedbackRepository;
import com.example.weeklyreport.repository.PasswordConnectionProvider;
import com.example.weeklyreport.service.WeeklyReportGenerator;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class WeeklyReportHandler implements RequestHandler<Map<String, Object>, String> {

    private final FeedbackRepository repository;
    private final WeeklyReportGenerator generator;
    private final HtmlReportFormatter formatter;
    private final ReportPublisher publisher;
    private final Clock clock;

    public WeeklyReportHandler() {
        this(System.getenv(), Clock.systemUTC());
    }

    WeeklyReportHandler(Map<String, String> env, Clock clock) {
        this(new JdbcFeedbackRepository(new PasswordConnectionProvider(
                        required(env, "DB_URL"), required(env, "DB_USER"), required(env, "DB_PASSWORD"))),
                new WeeklyReportGenerator(),
                new HtmlReportFormatter(),
                new SmtpReportPublisher(SmtpSettings.fromEnvironment(env)),
                clock);
    }

    public WeeklyReportHandler(FeedbackRepository repository, WeeklyReportGenerator generator,
                               HtmlReportFormatter formatter, ReportPublisher publisher, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        return run();
    }

    public String run() {
        ReportPeriod period = ReportPeriod.previousWeek(clock);
        log("Generating weekly report from " + period.startDate() + " to " + period.endDate().minusDays(1));

        List<Feedback> feedbacks = repository.findSubmittedWithin(period);
        WeeklyReport report = generator.generate(feedbacks, period);
        publisher.publish(formatter.format(report));

        String summary = String.format(Locale.ROOT,
                "Weekly report sent: period=%s..%s, feedbacks=%d, averageRating=%.2f",
                period.startDate(), period.endDate().minusDays(1), report.totalFeedbacks(), report.averageRating());
        log(summary);
        return summary;
    }

    private static void log(String message) {
        System.out.println("[weekly-report] " + message);
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " must be set");
        }
        return value;
    }
}