package br.com.fiap.tc4.notifications.email;

import br.com.fiap.tc4.notifications.model.CriticalNotification;
import br.com.fiap.tc4.notifications.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class SesNotificationSender implements NotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(SesNotificationSender.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final SesV2Client client;
    private final String from;
    private final List<String> recipients;

    SesNotificationSender(SesV2Client client, String from, List<String> recipients) {
        this.client = client;
        this.from = from;
        this.recipients = List.copyOf(recipients);
    }

    @Override
    public void send(CriticalNotification notification) {
        String body = """
                Uma avaliação crítica foi recebida.

                Descrição: %s
                Urgência: %s
                Data de envio: %s
                """.formatted(notification.description(), notification.urgency(),
                DATE_TIME.format(notification.submittedAt()));

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(destination -> destination.toAddresses(recipients))
                .content(content -> content.simple(message -> message
                        .subject(utf8("Avaliação crítica recebida"))
                        .body(messageBody -> messageBody.text(utf8(body)))))
                .build();
        SendEmailResponse response;
        try {
            response = client.sendEmail(request);
        } catch (SdkException e) {
            throw new NotificationDeliveryException(e);
        }
        LOGGER.info("critical_notification_sent messageId={} submittedAt={}",
                response.messageId(), notification.submittedAt());
    }

    private static Content utf8(String value) {
        return Content.builder().data(value).charset("UTF-8").build();
    }
}
