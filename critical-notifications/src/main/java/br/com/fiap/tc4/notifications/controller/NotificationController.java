package br.com.fiap.tc4.notifications.controller;

import br.com.fiap.tc4.notifications.dto.NotificationResponse;
import br.com.fiap.tc4.notifications.dto.NotificationRequest;

import br.com.fiap.tc4.notifications.service.CriticalNotificationService;
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

}
