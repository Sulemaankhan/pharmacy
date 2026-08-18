package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
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
        return toResponse(user);
    }

    public AuthResponse login(AuthRequest req) {
        User user = users.findByEmail(req.email() == null ? "" : req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toResponse(user);
    }

    public User requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user.getEmail()), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
