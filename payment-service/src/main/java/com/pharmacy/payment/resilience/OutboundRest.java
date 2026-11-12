package com.pharmacy.payment.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

public final class OutboundRest {
    private OutboundRest() {}

    public static RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
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
