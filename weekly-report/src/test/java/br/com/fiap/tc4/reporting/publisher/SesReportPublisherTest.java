package br.com.fiap.tc4.reporting.publisher;

import br.com.fiap.tc4.reporting.exception.ReportPublishingException;
import br.com.fiap.tc4.reporting.model.ReportEmail;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesReportPublisherTest {

    private static final ReportEmail EMAIL =
            new ReportEmail("Relatório semanal de feedbacks - teste", "<p>Média das avaliações: 7,50</p>");

    private final List<SendEmailRequest> requests = new ArrayList<>();

    private final SesV2Client recordingClient = new SesV2Client() {
        @Override
        public SendEmailResponse sendEmail(SendEmailRequest request) {
            requests.add(request);
            return SendEmailResponse.builder().messageId("test-id").build();
        }

        @Override
        public String serviceName() {
            return "ses";
        }

        @Override
        public void close() {
        }
    };

    @Test
    void sendsHtmlEmailToAllRecipients() {
        new SesReportPublisher(recordingClient, "reports@example.com",
                List.of("admin@example.com", "admin2@example.com")).publish(EMAIL);

        assertEquals(1, requests.size());
        SendEmailRequest request = requests.getFirst();
        assertEquals("reports@example.com", request.fromEmailAddress());
        assertEquals(List.of("admin@example.com", "admin2@example.com"), request.destination().toAddresses());
        assertEquals("Relatório semanal de feedbacks - teste", request.content().simple().subject().data());
        assertEquals("UTF-8", request.content().simple().subject().charset());
        assertEquals("<p>Média das avaliações: 7,50</p>", request.content().simple().body().html().data());
    }

    @Test
    void wrapsSesFailures() {
        SesV2Client failingClient = new SesV2Client() {
            @Override
            public SendEmailResponse sendEmail(SendEmailRequest request) {
                throw SesV2Exception.builder().message("Email address is not verified").build();
            }

            @Override
            public String serviceName() {
                return "ses";
            }

            @Override
            public void close() {
            }
        };

        ReportPublishingException exception = assertThrows(ReportPublishingException.class,
                () -> new SesReportPublisher(failingClient, "reports@example.com", List.of("admin@example.com"))
                        .publish(EMAIL));
        assertInstanceOf(SesV2Exception.class, exception.getCause());
    }

    @Test
    void rejectsEmptyRecipients() {
        assertThrows(IllegalArgumentException.class,
                () -> new SesReportPublisher(recordingClient, "reports@example.com", List.of()));
    }
}