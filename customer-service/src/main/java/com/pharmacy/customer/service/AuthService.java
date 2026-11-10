package com.pharmacy.customer.service;

import com.pharmacy.customer.dto.AuthRequest;
import com.pharmacy.customer.dto.AuthResponse;
import com.pharmacy.customer.dto.CustomerProfile;
import com.pharmacy.customer.dto.ForgotPasswordRequest;
import com.pharmacy.customer.dto.ProfileUpdateRequest;
import com.pharmacy.customer.dto.ResetPasswordRequest;
import com.pharmacy.customer.entity.PasswordResetToken;
import com.pharmacy.customer.entity.User;
import com.pharmacy.customer.kafka.NotificationPublisher;
import com.pharmacy.customer.repository.PasswordResetTokenRepository;
import com.pharmacy.customer.repository.UserRepository;
import com.pharmacy.customer.security.JwtService;
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
import java.util.Locale;
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
    private final NotificationPublisher notifications;
    private final String frontendBaseUrl;

    public AuthService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            PasswordEncoder encoder,
            JwtService jwt,
            NotificationPublisher notifications,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.notifications = notifications;
        this.frontendBaseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:5173" : frontendBaseUrl.trim().replaceAll("/$", "");
    }

    public AuthResponse register(AuthRequest req) {
        if (req.email() == null || req.password() == null || req.name() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email and password are required");
        }
        if (users.existsByEmail(req.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPassword(encoder.encode(req.password()));
        user.setRole("USER");
        users.save(user);
        log.info("Register success email={}", user.getEmail());
        return toAuth(user);
    }

    public AuthResponse login(AuthRequest req) {
        String email = req.email() == null ? "" : req.email().toLowerCase();
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        log.info("Login success email={}", user.getEmail());
        return toAuth(user);
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest req) {
        String email = req == null || req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your email ID");
        }
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
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
        notifications.passwordReset(user.getName(), user.getEmail(), frontendBaseUrl + "/reset-password?token=" + rawToken);
        return Map.of("status", "QUEUED", "message", GENERIC_RESET_MESSAGE);
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
        return Map.of("status", "UPDATED", "message", "Password updated. You can sign in with your new password.");
    }

    public User requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public CustomerProfile profile(User user) {
        return toProfile(user, null);
    }

    @Transactional
    public CustomerProfile update(User user, ProfileUpdateRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your name");
        }
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email ID");
        }
        String phone = digits(req.phone());
        if (!phone.isEmpty() && phone.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 10-digit contact number");
        }
        String pin = digits(req.pincode());
        if (!pin.isEmpty() && pin.length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 6-digit pincode");
        }
        if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(blankToNull(req.address()));
        user.setCity(blankToNull(req.city()));
        user.setState(blankToNull(req.state()));
        user.setPincode(pin.isEmpty() ? null : pin);
        user.setUpdatedAt(Instant.now());
        users.saveAndFlush(user);
        return toProfile(user, emailChanged ? jwt.generate(user.getEmail()) : null);
    }

    private AuthResponse toAuth(User user) {
        return new AuthResponse(jwt.generate(user.getEmail()), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private CustomerProfile toProfile(User user, String token) {
        return new CustomerProfile(
                user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getPhone(), user.getAddress(), user.getCity(), user.getState(), user.getPincode(),
                user.getCreatedAt(), user.getUpdatedAt(), token);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash reset token");
        }
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
