package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.dto.ForgotPasswordRequest;
import com.pharmacy.drugstore.dto.ResetPasswordRequest;
import com.pharmacy.drugstore.entity.PasswordResetToken;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.notification.MailClient;
import com.pharmacy.drugstore.notification.PasswordResetEmailNotification;
import com.pharmacy.drugstore.repository.PasswordResetTokenRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String GENERIC_RESET_MESSAGE =
            "If that email is registered, a password reset link was sent.";

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailClient mailClient;
    private final String frontendBaseUrl;

    public AuthService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            PasswordEncoder encoder,
            JwtService jwt,
            MailClient mailClient,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mailClient = mailClient;
        this.frontendBaseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:5173" : frontendBaseUrl.trim().replaceAll("/$", "");
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

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest req) {
        String email = req == null || req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your email ID");
        }
        log.info("Forgot password request email={}", email);
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Forgot password unknown email={}", email);
            return Map.of("status", "QUEUED", "message", GENERIC_RESET_MESSAGE);
        }
        for (PasswordResetToken previous : resetTokens.findByUserAndUsedAtIsNull(user)) {
            previous.setUsedAt(Instant.now());
            resetTokens.save(previous);
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        resetTokens.saveAndFlush(token);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + rawToken;
        new PasswordResetEmailNotification(mailClient).send(user, resetUrl);
        log.info("Forgot password email sent {}", RequestMdc.describe(user));
        return Map.of("status", "SENT", "message", GENERIC_RESET_MESSAGE);
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest req) {
        String rawToken = req == null || req.token() == null ? "" : req.token().trim();
        String password = req == null || req.password() == null ? "" : req.password();
        if (rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is missing or invalid");
        }
        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        PasswordResetToken token = resetTokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired"));
        if (token.getUsedAt() != null || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired");
        }
        User user = token.getUser();
        user.setPassword(encoder.encode(password));
        users.save(user);
        token.setUsedAt(Instant.now());
        resetTokens.save(token);
        RequestMdc.setUser(user);
        log.info("Password reset success {}", RequestMdc.describe(user));
        return Map.of("status", "UPDATED", "message", "Password updated. You can sign in with your new password.");
    }

    public User requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authenticated principal email={} not found", email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
                });
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash reset token");
        }
    }

    private AuthResponse toResponse(User user) {
        log.info("Issued JWT {}", RequestMdc.describe(user));
        return new AuthResponse(jwt.generate(user.getEmail()), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
