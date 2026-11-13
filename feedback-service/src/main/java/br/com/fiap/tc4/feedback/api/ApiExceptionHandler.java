package br.com.fiap.tc4.feedback.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * The charset is explicit because, in Lambda, the serverless container decodes response bodies
     * without a declared charset as ISO-8859-1, which would garble the accents in the message.
     */
    static final MediaType PROBLEM_JSON_UTF8 = new MediaType(MediaType.APPLICATION_PROBLEM_JSON, StandardCharsets.UTF_8);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalidRequest(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Informe descricao (texto não vazio, até " + FeedbackRequest.MAX_DESCRIPTION_LENGTH
                        + " caracteres) e nota (inteiro de 0 a 10).");
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON_UTF8).body(problem);
    }
}
