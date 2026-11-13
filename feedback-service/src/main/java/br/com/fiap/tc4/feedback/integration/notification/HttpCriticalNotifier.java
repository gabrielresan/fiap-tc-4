package br.com.fiap.tc4.feedback.integration.notification;

import br.com.fiap.tc4.feedback.service.CriticalNotifier;
import br.com.fiap.tc4.feedback.exception.NotificationException;

import br.com.fiap.tc4.feedback.model.Feedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import software.amazon.awssdk.core.exception.SdkException;

import java.net.http.HttpClient;

/**
 * Sends critical feedback to {@code POST /notifications} of the critical-notifications service.
 */
@Component
public class HttpCriticalNotifier implements CriticalNotifier {

    private final RestClient restClient;

    public HttpCriticalNotifier(NotificationsProperties properties,
                                @Value("${feedback.notifications.iam-region:}") String iamRegion) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.url().toString())
                .requestFactory(requestFactory);
        if (!iamRegion.isBlank()) {
            builder.requestInterceptor(new ApiGatewayRequestSigner(iamRegion));
        }
        this.restClient = builder.build();
    }

    @Override
    public void notify(Feedback feedback) {
        NotificationPayload payload = new NotificationPayload(
                feedback.description(),
                feedback.rating(),
                feedback.urgency().name(),
                feedback.submittedAt().toString());
        try {
            restClient.post()
                    .uri("/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | SdkException e) {
            throw new NotificationException("Failed to notify critical feedback " + feedback.id(), e);
        }
    }

    /**
     * Request body expected by critical-notifications. {@code submittedAt} is sent as ISO-8601.
     */
    record NotificationPayload(String description, int rating, String urgency, String submittedAt) {
    }
}
