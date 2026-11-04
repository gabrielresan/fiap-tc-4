package com.example.criticalnotifications;

import com.example.criticalnotifications.config.NotificationProperties;
import com.example.criticalnotifications.service.CriticalNotificationService;
import com.example.criticalnotifications.service.NotificationSender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(NotificationProperties.class)
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
