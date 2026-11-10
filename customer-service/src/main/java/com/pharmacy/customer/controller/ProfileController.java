package com.pharmacy.customer.controller;

import com.pharmacy.customer.dto.CustomerProfile;
import com.pharmacy.customer.dto.ProfileUpdateRequest;
import com.pharmacy.customer.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService auth;

    public ProfileController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping
    public CustomerProfile get(Authentication authentication) {
        return auth.profile(user(authentication));
    }

    @PatchMapping
    public CustomerProfile update(Authentication authentication, @RequestBody ProfileUpdateRequest request) {
        return auth.update(user(authentication), request);
    }

    private com.pharmacy.customer.entity.User user(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return auth.requireUser(authentication.getName());
    }
}
