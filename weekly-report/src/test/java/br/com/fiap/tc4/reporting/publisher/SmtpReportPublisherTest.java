package br.com.fiap.tc4.reporting.publisher;

import br.com.fiap.tc4.reporting.exception.ReportPublishingException;
import br.com.fiap.tc4.reporting.model.ReportEmail;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class SmtpReportPublisherTest {

    @Container
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.33.0")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/livez").forPort(8025));

    private final HttpClient http = HttpClient.newHttpClient();

    private static SmtpSettings settings(String host, int port) {
        return new SmtpSettings(host, port, "", "", false, false,
                "reports@example.com", List.of("admin@example.com", "admin2@example.com"));
    }

    private String latestMessageJson() throws Exception {
        URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025)
                + "/api/v1/message/latest");
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        return response.body();
    }

    @Test
    void sendsHtmlEmailToAllRecipients() throws Exception {
        SmtpReportPublisher publisher =
                new SmtpReportPublisher(settings(MAILPIT.getHost(), MAILPIT.getMappedPort(1025)));

        publisher.publish(new ReportEmail("Relatório semanal de feedbacks - teste",
                "<p>Média das avaliações: 7,50</p>"));

        String json = latestMessageJson();
        assertTrue(json.contains("\"Subject\":\"Relatório semanal de feedbacks - teste\""));
        assertTrue(json.contains("\"Address\":\"reports@example.com\""));
        assertTrue(json.contains("\"Address\":\"admin@example.com\""));
        assertTrue(json.contains("\"Address\":\"admin2@example.com\""));
        // Mailpit's JSON escapes "<" and ">" as \u003c and \u003e
        assertTrue(json.contains("\"HTML\":\"\\u003cp\\u003eMédia das avaliações: 7,50\\u003c/p\\u003e"));
    }

    @Test
    void wrapsDeliveryFailures() {
        // Nothing listens on local port 1, so the SMTP connection is refused
        SmtpReportPublisher publisher = new SmtpReportPublisher(settings("localhost", 1));

        ReportPublishingException exception = assertThrows(ReportPublishingException.class,
                () -> publisher.publish(new ReportEmail("subject", "<p>body</p>")));
        assertInstanceOf(MessagingException.class, exception.getCause());
    }

    @Test
    void rejectsInvalidAddresses() {
        SmtpSettings invalid = new SmtpSettings("localhost", 1025, "", "", false, false,
                "invalid-address", List.of("admin@example.com"));

        assertThrows(IllegalArgumentException.class, () -> new SmtpReportPublisher(invalid));
    }
}