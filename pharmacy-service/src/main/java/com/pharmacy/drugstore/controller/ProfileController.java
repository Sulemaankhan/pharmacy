package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.dto.ProfileUpdateRequest;
import com.pharmacy.drugstore.dto.UserProfileResponse;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.service.AuthService;
import com.pharmacy.drugstore.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService authService;
    private final ProfileService profiles;

    public ProfileController(AuthService authService, ProfileService profiles) {
        this.authService = authService;
        this.profiles = profiles;
    }

    @GetMapping
    public UserProfileResponse get(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return profiles.get(user(auth), authorization);
    }

    @PatchMapping
    public UserProfileResponse update(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ProfileUpdateRequest request) {
        return profiles.update(user(auth), authorization, request);
    }

    private User user(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return authService.requireUser(auth.getName());
    }
}
