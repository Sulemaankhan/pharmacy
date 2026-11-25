package com.pharmacy.payment.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OutboundRest {
    private static final ExecutorService HTTP_EXECUTOR = Executors.newFixedThreadPool(32);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .executor(HTTP_EXECUTOR)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private OutboundRest() {}

    public static RestClient client(String baseUrl) {
        return client(baseUrl, Duration.ofSeconds(8));
    }

    public static RestClient client(String baseUrl, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HTTP_CLIENT);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public static String reason(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return current.getClass().getSimpleName() + ": " + message;
    }

    public static boolean open(Throwable ex) {
        return ex instanceof CallNotPermittedException;
    }
}
