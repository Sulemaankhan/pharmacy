package com.pharmacy.cloudgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;

@Configuration
public class CorsConfig {
    private static final String ALLOW_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD";
    private static final String DEFAULT_ALLOW_HEADERS =
            "Authorization, Content-Type, X-Request-Id, Accept, Origin, X-Requested-With";
    private static final List<String> CORS_HEADERS = List.of(
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            HttpHeaders.ACCESS_CONTROL_MAX_AGE);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsWebFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            applyCors(request, response.getHeaders());
            if (CorsUtils.isPreFlightRequest(request) || request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }
            response.beforeCommit(() -> {
                applyCors(request, response.getHeaders());
                return Mono.empty();
            });
            return chain.filter(exchange);
        };
    }

    private static void applyCors(ServerHttpRequest request, HttpHeaders headers) {
        try {
            CORS_HEADERS.forEach(headers::remove);
            String origin = request.getHeaders().getOrigin();
            if (origin != null && !origin.isBlank()) {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                headers.set(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            } else {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_METHODS);
            String requested = request.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    requested == null || requested.isBlank() ? DEFAULT_ALLOW_HEADERS : requested);
            headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    "Content-Disposition, X-Request-Id, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, X-Throttle-Limit, X-Throttle-Remaining, Retry-After");
            headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "86400");
        } catch (UnsupportedOperationException ignored) {
            // Netty may freeze headers at commit; values were already written above.
        }
    }
}
