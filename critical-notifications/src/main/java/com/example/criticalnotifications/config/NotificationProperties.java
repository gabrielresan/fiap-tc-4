package com.example.criticalnotifications.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("notifications")
public record NotificationProperties(
        @NotBlank @Email String from,
        @NotEmpty List<@NotBlank @Email String> recipients
) {
}
