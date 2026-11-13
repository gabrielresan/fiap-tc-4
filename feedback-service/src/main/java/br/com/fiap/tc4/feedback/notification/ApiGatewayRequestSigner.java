package br.com.fiap.tc4.feedback.notification;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

import java.io.IOException;

final class ApiGatewayRequestSigner implements ClientHttpRequestInterceptor {
    private final String region;
    private final AwsCredentialsProvider credentials;
    private final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

    ApiGatewayRequestSigner(String region) {
        this(region, DefaultCredentialsProvider.builder().build());
    }

    ApiGatewayRequestSigner(String region, AwsCredentialsProvider credentials) {
        this.region = region;
        this.credentials = credentials;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        SdkHttpRequest unsigned = SdkHttpRequest.builder()
                .uri(request.getURI())
                .method(SdkHttpMethod.fromValue(request.getMethod().name()))
                .build();
        SignedRequest signed = signer.sign(sign -> sign
                .identity(credentials.resolveCredentials())
                .request(unsigned)
                .payload(ContentStreamProvider.fromByteArray(body))
                .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api"));

        signed.request().headers().forEach((name, values) -> {
            // The JDK HTTP client sets Host from the request URI.
            if (!"Host".equalsIgnoreCase(name)) {
                request.getHeaders().put(name, values);
            }
        });
        return execution.execute(request, body);
    }
}
