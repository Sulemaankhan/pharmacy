package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(AuthRequest req) {
        log.info("Register attempt name={} email={}", req.name(), req.email());
        if (req.email() == null || req.password() == null || req.name() == null) {
            log.warn("Register rejected missing fields name={} email={}", req.name(), req.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email and password are required");
        }
        if (users.existsByEmail(req.email().toLowerCase())) {
            log.warn("Register conflict email={}", req.email().toLowerCase());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPassword(encoder.encode(req.password()));
        user.setRole("USER");
        users.save(user);
        RequestMdc.setUser(user);
        log.info("Register success {}", RequestMdc.describe(user));
        return toResponse(user);
    }

    public AuthResponse login(AuthRequest req) {
        String email = req.email() == null ? "" : req.email().toLowerCase();
        log.info("Login attempt email={}", email);
        User user = users.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed unknown email={}", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });
        if (!encoder.matches(req.password(), user.getPassword())) {
            log.warn("Login failed bad password {}", RequestMdc.describe(user));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        RequestMdc.setUser(user);
        log.info("Login success {}", RequestMdc.describe(user));
        return toResponse(user);
    }

    public User requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authenticated principal email={} not found", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
                });
    }

    private AuthResponse toResponse(User user) {
        log.info("Issued JWT {}", RequestMdc.describe(user));
        return new AuthResponse(jwt.generate(user.getEmail()), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
