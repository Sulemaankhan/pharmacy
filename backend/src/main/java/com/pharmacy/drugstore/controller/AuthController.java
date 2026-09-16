package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.dto.AuthRequest;
import com.pharmacy.drugstore.dto.AuthResponse;
import com.pharmacy.drugstore.dto.ForgotPasswordRequest;
import com.pharmacy.drugstore.dto.ResetPasswordRequest;
import com.pharmacy.drugstore.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
