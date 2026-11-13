package br.com.fiap.tc4.reporting.integration.email;

import br.com.fiap.tc4.reporting.exception.ReportPublishingException;
import br.com.fiap.tc4.reporting.model.ReportEmail;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class SesReportPublisher implements ReportPublisher {

    private final SesV2Client client;
    private final String from;
    private final List<String> recipients;

    public SesReportPublisher(SesV2Client client, String from, List<String> recipients) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.from = Objects.requireNonNull(from, "from must not be null");
        this.recipients = List.copyOf(recipients);
        if (this.recipients.isEmpty()) {
            throw new IllegalArgumentException("recipients must not be empty");
        }
    }

    @Override
    public void publish(ReportEmail email) {
        Objects.requireNonNull(email, "email must not be null");
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(recipients).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(utf8(email.subject()))
                                .body(Body.builder().html(utf8(email.htmlBody())).build())
                                .build())
                        .build())
                .build();
        try {
            client.sendEmail(request);
        } catch (SdkException e) {
            throw new ReportPublishingException("Failed to send weekly report e-mail via Amazon SES", e);
        }
    }

    private static Content utf8(String text) {
        return Content.builder().data(text).charset(StandardCharsets.UTF_8.name()).build();
    }
}