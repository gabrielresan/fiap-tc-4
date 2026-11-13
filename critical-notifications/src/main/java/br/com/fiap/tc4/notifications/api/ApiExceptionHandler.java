package br.com.fiap.tc4.notifications.api;

import br.com.fiap.tc4.notifications.email.NotificationDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotificationDeliveryException.class)
    ProblemDetail deliveryFailed(NotificationDeliveryException exception) {
        LOGGER.error("critical_notification_delivery_failed", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Não foi possível enviar o aviso de avaliação crítica.");
    }


    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ProblemDetail invalidRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Informe description, rating de 0 a 10, urgency LOW, MEDIUM ou CRITICAL e submittedAt em formato ISO-8601.");
    }
}
