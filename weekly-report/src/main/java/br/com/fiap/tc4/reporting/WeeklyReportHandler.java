package br.com.fiap.tc4.reporting;

import br.com.fiap.tc4.reporting.dto.WeeklyReportResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import br.com.fiap.tc4.reporting.integration.formatting.HtmlReportFormatter;
import br.com.fiap.tc4.reporting.integration.email.ReportPublisher;
import br.com.fiap.tc4.reporting.integration.email.SesReportPublisher;
import br.com.fiap.tc4.reporting.integration.email.SmtpReportPublisher;
import br.com.fiap.tc4.reporting.integration.email.SmtpSettings;
import br.com.fiap.tc4.reporting.repository.FeedbackRepository;
import br.com.fiap.tc4.reporting.repository.JdbcFeedbackRepository;
import br.com.fiap.tc4.reporting.repository.PasswordConnectionProvider;
import br.com.fiap.tc4.reporting.service.WeeklyReportGenerator;
import br.com.fiap.tc4.reporting.service.WeeklyReportService;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;

public class WeeklyReportHandler implements RequestHandler<Map<String, Object>, Object> {

    private final WeeklyReportService service;

    public WeeklyReportHandler() {
        this(System.getenv(), Clock.systemUTC());
    }

    WeeklyReportHandler(Map<String, String> env, Clock clock) {
        this(new JdbcFeedbackRepository(new PasswordConnectionProvider(
                        required(env, "DB_URL"), required(env, "DB_USER"), required(env, "DB_PASSWORD"))),
                new WeeklyReportGenerator(),
                new HtmlReportFormatter(),
                publisher(env),
                clock);
    }

    static ReportPublisher publisher(Map<String, String> env) {
        SmtpSettings settings = SmtpSettings.fromEnvironment(env);
        String provider = env.getOrDefault("EMAIL_PROVIDER", "smtp");
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "smtp" -> new SmtpReportPublisher(settings);
            case "ses" -> new SesReportPublisher(
                    SesV2Client.builder().httpClient(UrlConnectionHttpClient.create()).build(),
                    settings.from(),
                    settings.recipients());
            default -> throw new IllegalStateException("EMAIL_PROVIDER must be smtp or ses, got " + provider);
        };
    }

    public WeeklyReportHandler(FeedbackRepository repository, WeeklyReportGenerator generator,
                               HtmlReportFormatter formatter, ReportPublisher publisher, Clock clock) {
        this.service = new WeeklyReportService(repository, generator, formatter, publisher, clock);
    }

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        WeeklyReportResponse response = sendReport();
        return input != null && "json".equals(input.get("responseFormat")) ? response : response.summary();
    }

    public String run() {
        return sendReport().summary();
    }

    private WeeklyReportResponse sendReport() {
        WeeklyReportResponse response = WeeklyReportResponse.from(service.sendPreviousWeek());
        System.out.println("[weekly-report] " + response.summary());
        return response;
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " must be set");
        }
        return value;
    }
}