package br.com.fiap.tc4.feedback.controller;

import br.com.fiap.tc4.feedback.dto.FeedbackRequest;
import br.com.fiap.tc4.feedback.dto.FeedbackResponse;

import br.com.fiap.tc4.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacao")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody FeedbackRequest request) {
        return FeedbackResponse.from(service.submit(request.descricao(), request.nota()));
    }
}
