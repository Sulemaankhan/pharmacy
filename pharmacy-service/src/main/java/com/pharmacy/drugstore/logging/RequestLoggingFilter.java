package com.pharmacy.drugstore.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/actuator") || "OPTIONS".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(RequestMdc.HEADER);
        String requestId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming.trim();
        RequestMdc.setRequestId(requestId);
        RequestMdc.setAnonymous();
        response.setHeader(RequestMdc.HEADER, requestId);

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        log.info("HTTP start {} {}{} ip={} ua={}",
                method,
                path,
                query == null ? "" : "?" + query,
                clientIp(request),
                dash(request.getHeader("User-Agent")));
        try {
            chain.doFilter(request, response);
            log.info("HTTP end {} {} status={} durationMs={}",
                    method, path, response.getStatus(), System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("HTTP failed {} {} durationMs={} error={}",
                    method, path, System.currentTimeMillis() - start, ex.getMessage(), ex);
            throw ex;
        } finally {
            RequestMdc.clear();
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return dash(request.getRemoteAddr());
    }

    private String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
