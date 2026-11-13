package br.com.fiap.tc4.notifications.integration.email;

import br.com.fiap.tc4.notifications.exception.NotificationDeliveryException;

import br.com.fiap.tc4.notifications.model.CriticalNotification;
import br.com.fiap.tc4.notifications.model.Urgency;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesNotificationSenderTest {
    private final SesV2Client client = mock(SesV2Client.class);
    private final List<String> recipients = List.of("admin@example.com", "team@example.com");
    private final SesNotificationSender sender = new SesNotificationSender(client, "reports@example.com", recipients);
    private final CriticalNotification notification = new CriticalNotification(
            "Vídeo indisponível", Urgency.CRITICAL, Instant.parse("2026-11-13T19:00:00Z"),
            Instant.parse("2026-11-13T19:00:01Z"));

    @Test
    void sendsRequiredDetailsToConfiguredAdministrators() {
        when(client.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("test-message").build());

        sender.send(notification);

        var captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(client).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo("reports@example.com");
        assertThat(request.destination().toAddresses()).containsExactlyElementsOf(recipients);
        assertThat(request.content().simple().body().text().data())
                .contains("Vídeo indisponível", "CRITICAL", "13/11/2026 16:00:00 -03:00");
        assertThat(request.content().simple().body().text().charset()).isEqualTo("UTF-8");
    }

    @Test
    void propagatesTheOriginalSesFailure() {
        var rejection = MessageRejectedException.builder().message("Email address is not verified").build();
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(rejection);

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(rejection);
    }
}
