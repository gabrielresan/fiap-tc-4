package br.com.fiap.tc4.notifications.integration.email;

import br.com.fiap.tc4.notifications.service.NotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "notifications.email-provider", havingValue = "ses")
class SesNotificationConfiguration {
    @Bean
    SesV2Client sesClient() {
        return SesV2Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(8)))
                .build();
    }

    @Bean
    NotificationSender sesNotificationSender(SesV2Client client,
            @Value("${NOTIFICATION_FROM}") String from, @Value("${NOTIFICATION_RECIPIENTS}") String recipients) {
        List<String> addresses = Arrays.stream(recipients.split(","))
                .map(String::strip)
                .filter(address -> !address.isEmpty())
                .toList();
        if (from.isBlank() || addresses.isEmpty()) {
            throw new IllegalArgumentException("SES sender and recipients must be configured");
        }
        return new SesNotificationSender(client, from.strip(), addresses);
    }
}
