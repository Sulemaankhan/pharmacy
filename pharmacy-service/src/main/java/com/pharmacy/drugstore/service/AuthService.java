package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.client.CustomerClient;
import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.dto.ForgotPasswordRequest;
import com.pharmacy.drugstore.dto.ResetPasswordRequest;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final CustomerClient customers;

    public AuthService(UserRepository users, CustomerClient customers) {
        this.users = users;
        this.customers = customers;
    }

    public AuthResponse register(AuthRequest req) {
        log.info("Register attempt name={} email={}", req.name(), req.email());
        AuthResponse response = customers.register(req);
        User user = users.findByEmail(response.email()).orElse(null);
        if (user != null) {
            RequestMdc.setUser(user);
        }
        log.info("Register success email={}", response.email());
        return response;
    }

    public AuthResponse login(AuthRequest req) {
        String email = req.email() == null ? "" : req.email().toLowerCase();
        log.info("Login attempt email={}", email);
        AuthResponse response = customers.login(req);
        User user = users.findByEmail(response.email()).orElse(null);
        if (user != null) {
            RequestMdc.setUser(user);
        }
        log.info("Login success email={}", response.email());
        return response;
    }

    public Map<String, String> forgotPassword(ForgotPasswordRequest req) {
        String email = req == null || req.email() == null ? "" : req.email().trim().toLowerCase();
        log.info("Forgot password request email={}", email);
        return customers.forgotPassword(req);
    }

    public Map<String, String> resetPassword(ResetPasswordRequest req) {
        log.info("Password reset attempt");
        return customers.resetPassword(req);
    }

    public User requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authenticated principal email={} not found", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
                });
    }
}
