package com.example.criticalnotifications.api;

import com.example.criticalnotifications.model.CriticalNotification;
import com.example.criticalnotifications.model.Urgency;
import com.example.criticalnotifications.service.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.argThat;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "notifications.from=notifications@example.com",
        "notifications.recipients=admin@example.com"
})
class NotificationApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationSender sender;

    @BeforeEach
    void resetSender() {
        reset(sender);
    }

    @Test
    void sendsCriticalNotificationThroughHttp() throws Exception {
        Instant before = Instant.now();
        MockHttpServletResponse response = post(payload("CRITICAL", "1"));
        Instant after = Instant.now();

        assertEquals(200, response.getStatus());
        assertEquals("{\"sent\":true}", response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        verify(sender).send(argThat(notification ->
                notification.description().equals("Aula indisponível")
                        && notification.urgency() == Urgency.CRITICAL
                        && notification.submittedAt().equals(Instant.parse("2026-11-04T12:00:00Z"))
                        && !notification.sentAt().isBefore(before)
                        && !notification.sentAt().isAfter(after)));
    }

    @Test
    void ignoresNonCriticalFeedback() throws Exception {
        for (String urgency : new String[]{"LOW", "MEDIUM"}) {
            MockHttpServletResponse response = post(payload(urgency, "0"));
            assertEquals(200, response.getStatus());
            assertEquals("{\"sent\":false}", response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }
        verifyNoInteractions(sender);
    }

    @Test
    void rejectsMissingFieldsAndInvalidInputWithoutSending() throws Exception {
        String valid = payload("CRITICAL", "1");
        String[] invalidBodies = {
                "{}", "null", "{", "[]", "",
                payload("UNKNOWN", "1"), payload("CRITICAL", "null"),
                payload("CRITICAL", "-1"), payload("CRITICAL", "11"), payload("CRITICAL", "1.5"),
                valid.replace("Aula indisponível", "   "),
                valid.replace("2026-11-04T12:00:00Z", "invalid-date"),
                valid.replace("\"submittedAt\":\"2026-11-04T12:00:00Z\"", "\"submittedAt\":null"),
                valid.replace("\"rating\":1,", "")
        };
        for (String body : invalidBodies) {
            assertEquals(400, post(body).getStatus(), body);
        }
        verifyNoInteractions(sender);
    }

    @Test
    void returnsServiceUnavailableWhenDeliveryFails() throws Exception {
        doThrow(new MailSendException("private SMTP details")).when(sender).send(any(CriticalNotification.class));

        MockHttpServletResponse response = post(payload("CRITICAL", "1"));

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentType().contains("application/problem+json"));
        assertFalse(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("private SMTP details"));
        assertFalse(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("\"sent\":true"));
    }

    private MockHttpServletResponse post(String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/notifications")
                .contentType("application/json")
                .content(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .andReturn().getResponse();
    }

    private static String payload(String urgency, String rating) {
        return """
                {"description":"Aula indisponível","rating":%s,"urgency":"%s","submittedAt":"2026-11-04T12:00:00Z"}
                """.formatted(rating, urgency);
    }
}
