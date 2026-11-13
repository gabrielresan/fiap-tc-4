package br.com.fiap.tc4.feedback.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ApiGatewayRequestSignerTest {
    @Test
    void signsThePostWithTheExecutionRolesSessionToken() {
        var credentials = StaticCredentialsProvider.create(AwsSessionCredentials.create(
                "test-access-key", "test-secret-key", "test-session-token"));
        var builder = RestClient.builder()
                .baseUrl("https://example.execute-api.us-east-2.amazonaws.com")
                .requestInterceptor(new ApiGatewayRequestSigner("us-east-2", credentials));
        var server = MockRestServiceServer.bindTo(builder).build();
        String body = """
                {"description":"Aula indisponível","rating":2}
                """;
        server.expect(requestTo("https://example.execute-api.us-east-2.amazonaws.com/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", containsString("/us-east-2/execute-api/aws4_request")))
                .andExpect(header("X-Amz-Security-Token", "test-session-token"))
                .andExpect(content().string(body))
                .andRespond(withStatus(HttpStatus.OK));

        builder.build().post().uri("/notifications").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();

        server.verify();
    }
}
