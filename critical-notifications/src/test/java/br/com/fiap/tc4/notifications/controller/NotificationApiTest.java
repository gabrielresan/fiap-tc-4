package br.com.fiap.tc4.notifications.controller;

import br.com.fiap.tc4.notifications.model.Urgency;
import br.com.fiap.tc4.notifications.service.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.argThat;

@AutoConfigureMockMvc
@SpringBootTest
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
    void logsCriticalNotificationThroughHttp() throws Exception {
        Instant before = Instant.now();
        MockHttpServletResponse response = post(payload("CRITICAL", "1"));
        Instant after = Instant.now();

        assertEquals(200, response.getStatus());
        assertEquals("{\"processed\":true,\"logged\":true}", response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        verify(sender).send(argThat(notification ->
                notification.description().equals("Aula indisponível")
                        && notification.urgency() == Urgency.CRITICAL
                        && notification.submittedAt().equals(Instant.parse("2026-11-04T12:00:00Z"))
                        && !notification.loggedAt().isBefore(before)
                        && !notification.loggedAt().isAfter(after)));
    }

    @Test
    void ignoresNonCriticalFeedback() throws Exception {
        for (String urgency : new String[]{"LOW", "MEDIUM"}) {
            MockHttpServletResponse response = post(payload(urgency, "0"));
            assertEquals(200, response.getStatus());
            assertEquals("{\"processed\":false,\"logged\":false}", response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
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
            var response = post(body);
            assertEquals(400, response.getStatus(), body);
            assertTrue(response.getContentType().startsWith("application/problem+json"));
            assertTrue(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                    .contains("\"code\":\"INVALID_REQUEST\""));
        }
        verifyNoInteractions(sender);
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
