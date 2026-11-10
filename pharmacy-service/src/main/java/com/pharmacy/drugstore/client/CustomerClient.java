package com.pharmacy.drugstore.client;

import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.dto.CustomerProfile;
import com.pharmacy.drugstore.dto.ForgotPasswordRequest;
import com.pharmacy.drugstore.dto.ProfileUpdateRequest;
import com.pharmacy.drugstore.dto.ResetPasswordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Component
public class CustomerClient {
    private static final Logger log = LoggerFactory.getLogger(CustomerClient.class);
    private final RestClient rest;

    public CustomerClient(@Value("${app.customer-service.base-url:http://localhost:8087}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public AuthResponse register(AuthRequest request) {
        return post("/api/auth/register", request, AuthResponse.class);
    }

    public AuthResponse login(AuthRequest request) {
        return post("/api/auth/login", request, AuthResponse.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        return post("/api/auth/forgot-password", request, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        return post("/api/auth/reset-password", request, Map.class);
    }

    public CustomerProfile profile(String bearerToken) {
        try {
            return rest.get()
                    .uri("/api/profile")
                    .header("Authorization", bearer(bearerToken))
                    .retrieve()
                    .body(CustomerProfile.class);
        } catch (Exception ex) {
            throw translate(ex, "Customer service is unavailable. Start customer-service on port 8087.");
        }
    }

    public CustomerProfile updateProfile(String bearerToken, ProfileUpdateRequest request) {
        try {
            return rest.patch()
                    .uri("/api/profile")
                    .header("Authorization", bearer(bearerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CustomerProfile.class);
        } catch (Exception ex) {
            throw translate(ex, "Customer service is unavailable. Start customer-service on port 8087.");
        }
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            return rest.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
        } catch (Exception ex) {
            throw translate(ex, "Customer service is unavailable. Start customer-service on port 8087.");
        }
    }

    private static String bearer(String token) {
        if (token == null) {
            return "";
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

    private RuntimeException translate(Exception ex, String unavailable) {
        if (ex instanceof RestClientResponseException response) {
            String message = response.getResponseBodyAsString();
            if (message != null && message.contains("\"message\"")) {
                int start = message.indexOf("\"message\"") + 11;
                int from = message.indexOf('"', start);
                int to = message.indexOf('"', from + 1);
                if (from >= 0 && to > from) {
                    message = message.substring(from + 1, to);
                }
            }
            log.warn("Customer service HTTP {} {}", response.getStatusCode().value(), message);
            return new ResponseStatusException(HttpStatus.valueOf(response.getStatusCode().value()),
                    message == null || message.isBlank() ? "Customer request failed" : message);
        }
        log.error("Customer service call failed", ex);
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, unavailable);
    }
}
