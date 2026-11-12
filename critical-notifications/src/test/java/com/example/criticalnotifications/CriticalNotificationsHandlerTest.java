package com.example.criticalnotifications;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CriticalNotificationsHandlerTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final CriticalNotificationsHandler handler = new CriticalNotificationsHandler();

    @ParameterizedTest
    @CsvSource({"CRITICAL, 200, true", "LOW, 200, false", "INVALID, 400, false"})
    void handlesApiGatewayPayloadV2(String urgency, int statusCode, boolean logged) throws Exception {
        String body = """
                {"description":"Aula interrompida","rating":2,"urgency":"%s","submittedAt":"2026-11-09T12:00:00Z"}
                """.formatted(urgency);
        String event = """
                {
                  "version":"2.0",
                  "routeKey":"POST /notifications",
                  "rawPath":"/notifications",
                  "rawQueryString":"",
                  "headers":{"content-type":"application/json","host":"api.example.com"},
                  "requestContext":{
                    "domainName":"api.example.com",
                    "stage":"$default",
                    "requestId":"test-request",
                    "http":{"method":"POST","path":"/notifications","protocol":"HTTP/1.1",
                            "sourceIp":"127.0.0.1","userAgent":"test"}
                  },
                  "body":%s,
                  "isBase64Encoded":false
                }
                """.formatted(mapper.writeValueAsString(body));

        Context context = mock(Context.class);
        when(context.getAwsRequestId()).thenReturn("test-request");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(event.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                output, context);

        JsonNode response = mapper.readTree(output.toByteArray());
        assertEquals(statusCode, response.path("statusCode").intValue());
        if (statusCode == 200) {
            JsonNode responseBody = mapper.readTree(response.path("body").asText());
            assertEquals(logged ? "true" : "false", responseBody.path("logged").asText());
        }
    }
}
