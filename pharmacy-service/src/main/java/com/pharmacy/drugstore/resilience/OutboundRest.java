package com.pharmacy.drugstore.resilience;

import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class OutboundRest {
    private OutboundRest() {}

    public static RestClient client(RestClient.Builder builder, String baseUrl) {
        return client(builder, baseUrl, Duration.ofSeconds(5));
    }

    public static RestClient client(RestClient.Builder builder, String baseUrl, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
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
}
