package com.pharmacy.cloudgateway.filter;


import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.pharmacy.cloudgateway.ratelimit.RateLimitProperties;
import com.pharmacy.cloudgateway.ratelimit.TokenBucket;
import com.pharmacy.cloudgateway.ratelimit.TokenBucketRegistry;
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
public class RateLimitGatewayFilter implements org.springframework.cloud.gateway.filter.GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RateLimitGatewayFilter.class);
    private final RateLimitProperties properties;
    private final TokenBucketRegistry registry;
    private final MeterRegistry meters;

    public RateLimitGatewayFilter(
            RateLimitProperties properties,
            TokenBucketRegistry registry,
            MeterRegistry meters) {
        this.properties = properties;
        this.registry = registry;
        this.meters = meters;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (!properties.isEnabled() || skip(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        boolean auth = path.startsWith("/api/auth");
        RateLimitProperties.Policy policy = auth ? properties.getAuthPolicy() : properties.getDefaultPolicy();
        String policyName = auth ? "auth" : "default";
        String key = policyName + ":" + clientKey(exchange.getRequest(), auth);
        TokenBucket.Snapshot snapshot = registry.consume(key, policy);
        applyRateLimitHeaders(exchange, snapshot);
        if (snapshot.allowed()) {
            return chain.filter(exchange);
        }
        meters.counter("gateway.rate_limit.rejected", "policy", policyName).increment();
        log.warn("Token bucket exhausted policy={} key={} retryAfter={}s", policyName, key, snapshot.retryAfterSeconds());
        return tooManyRequests(exchange, snapshot);
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

    private String clientKey(ServerHttpRequest request, boolean auth) {
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

    private static void applyRateLimitHeaders(ServerWebExchange exchange, TokenBucket.Snapshot snapshot) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("X-RateLimit-Limit", Integer.toString(snapshot.limit()));
        headers.set("X-RateLimit-Remaining", Integer.toString(Math.max(0, snapshot.remaining())));
        headers.set("X-RateLimit-Reset", Long.toString((System.currentTimeMillis() / 1000) + snapshot.retryAfterSeconds()));
        if (!snapshot.allowed()) {
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(snapshot.retryAfterSeconds()));
        }
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, TokenBucket.Snapshot snapshot) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"message\":\"Too many requests. Please retry later.\",\"retryAfterSeconds\":"
                + snapshot.retryAfterSeconds() + "}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
