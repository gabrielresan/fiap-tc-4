package com.example.feedbackservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda entry point. Receives API Gateway HTTP API (payload 2.0) events and forwards them to the
 * same Spring MVC application that runs locally, started with the {@code lambda} profile.
 *
 * <p>Spring starts once per Lambda instance, during the init phase, and is reused by later invocations.
 */
public class FeedbackServiceHandler implements RequestStreamHandler {

    static final String LAMBDA_PROFILE = "lambda";

    /** Variables without local defaults in the cloud: the function fails to start if any is missing. */
    static final List<String> REQUIRED_ENV = List.of("DB_URL", "DB_USER", "DB_PASSWORD", "NOTIFICATIONS_URL");

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER =
            createHandler(System.getenv());

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        HANDLER.proxyStream(input, output, context);
    }

    static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> createHandler(
            Map<String, String> env) {
        REQUIRED_ENV.forEach(name -> required(env, name));
        try {
            return SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(
                    FeedbackServiceApplication.class, LAMBDA_PROFILE);
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Failed to start feedback-service in Lambda", e);
        }
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " must be set");
        }
        return value;
    }
}
