package com.pharmacy.customer.controller;

import com.pharmacy.customer.dto.AuthRequest;
import com.pharmacy.customer.dto.AuthResponse;
import com.pharmacy.customer.dto.ForgotPasswordRequest;
import com.pharmacy.customer.dto.ResetPasswordRequest;
import com.pharmacy.customer.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return auth.login(request);
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return auth.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return auth.resetPassword(request);
    }
}
