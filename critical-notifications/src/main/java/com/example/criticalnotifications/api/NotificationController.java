package com.example.criticalnotifications.api;

import com.example.criticalnotifications.service.CriticalNotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final CriticalNotificationService service;

    public NotificationController(CriticalNotificationService service) {
        this.service = service;
    }

    @PostMapping
    public NotificationResponse notify(@Valid @RequestBody NotificationRequest request) {
        return new NotificationResponse(service.notify(request.toFeedback()));
    }

    public record NotificationResponse(boolean logged) {
    }
}
