package com.pharmacy.cloudgateway.filter;


import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.pharmacy.cloudgateway.throttle.ConcurrencyThrottle;
import com.pharmacy.cloudgateway.throttle.ThrottleProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ThrottleGatewayFilter implements org.springframework.cloud.gateway.filter.GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(ThrottleGatewayFilter.class);

    private final ThrottleProperties properties;
    private final ConcurrencyThrottle throttle;
    private final MeterRegistry meters;

    public ThrottleGatewayFilter(
            ThrottleProperties properties,
            ConcurrencyThrottle throttle,
            MeterRegistry meters) {
        this.properties = properties;
        this.throttle = throttle;
        this.meters = meters;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (!properties.isEnabled() || skip(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        String key = clientKey(exchange.getRequest());
        return throttle.acquire(key).flatMap(acquired -> {
            applyThrottleHeaders(exchange, key);
            if (!acquired) {
                meters.counter("gateway.throttle.rejected").increment();
                log.warn("Request throttled key={}", key);
                return tooManyRequests(exchange);
            }
            return chain.filter(exchange).doFinally(signal -> throttle.release(key));
        });
    }

    private static boolean skip(ServerHttpRequest request) {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return true;
        }
        String path = request.getPath().value();
        return path == null || "/".equals(path)
                || path.startsWith("/actuator")
                || path.startsWith("/ws/");
    }

    private String clientKey(ServerHttpRequest request) {
        String path = request.getPath().value();
        boolean auth = path != null && path.startsWith("/api/auth");
        if (!auth) {
            String subject = jwtSubject(request);
            if (subject != null) {
                return "user:" + subject;
            }
        }
        return "ip:" + clientIp(request);
    }

    private static String jwtSubject(ServerHttpRequest request) {
        String email = request.getHeaders().getFirst("X-User-Email");
        return email == null || email.isBlank() ? null : email;
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private void applyThrottleHeaders(ServerWebExchange exchange, String key) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("X-Throttle-Limit", Integer.toString(throttle.clientLimit(key)));
        headers.set("X-Throttle-Remaining", Integer.toString(throttle.clientRemaining(key)));
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
        byte[] body = "{\"message\":\"Traffic throttled. Please retry later.\",\"retryAfterSeconds\":1}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
