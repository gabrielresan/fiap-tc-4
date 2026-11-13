package br.com.fiap.tc4.notifications;

import br.com.fiap.tc4.notifications.service.CriticalNotificationService;
import br.com.fiap.tc4.notifications.service.NotificationSender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class CriticalNotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CriticalNotificationsApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CriticalNotificationService criticalNotificationService(NotificationSender sender, Clock clock) {
        return new CriticalNotificationService(sender, clock);
    }
}
