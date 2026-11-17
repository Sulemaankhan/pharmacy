package com.pharmacy.cloudgateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthGatewayFilter implements org.springframework.cloud.gateway.filter.GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/auth",
            "/actuator",
            "/");
    private static final Set<HttpMethod> PUBLIC_METHODS = Set.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD);

    private final SecretKey key;

    public JwtAuthGatewayFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (isPublic(path, method)) {
            log.info("Request Route from cloudgateway.... {} {}", method, path);
            return chain.filter(exchange);
        }
        String token = bearer(exchange);
        if (token == null) {
            token = exchange.getRequest().getQueryParams().getFirst("token");
        }
        if (token == null || token.isBlank()) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String email = claims.getSubject();
            log.info("Request Route from cloudgateway.... {} {} user={}", method, path, email);
            if (isWebSocket(exchange, path)) {
                return chain.filter(exchange);
            }
            ServerWebExchange modified = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        if (email != null && !email.isBlank()) {
                            headers.set("X-User-Email", email);
                        }
                    }))
                    .build();
            return chain.filter(modified);
        } catch (Exception ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path == null) {
            return false;
        }
        if ("/".equals(path)) {
            return true;
        }
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(prefix -> !"/".equals(prefix)
                && (path.equals(prefix) || path.startsWith(prefix + "/")))) {
            return true;
        }
        return PUBLIC_METHODS.contains(method)
                && (path.startsWith("/api/products") || path.startsWith("/api/categories"));
    }

    private static boolean isWebSocket(ServerWebExchange exchange, String path) {
        String upgrade = exchange.getRequest().getHeaders().getUpgrade();
        return "websocket".equalsIgnoreCase(upgrade) || (path != null && path.startsWith("/ws/"));
    }

    private static String bearer(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
