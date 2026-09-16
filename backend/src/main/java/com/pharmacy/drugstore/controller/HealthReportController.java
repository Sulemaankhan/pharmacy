package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.dto.HealthReportResponse;
import com.pharmacy.drugstore.dto.HealthStatusResponse;
import com.pharmacy.drugstore.dto.HealthUploadResponse;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.service.AuthService;
import com.pharmacy.drugstore.service.HealthReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/health-reports")
public class HealthReportController {
    private final AuthService authService;
    private final HealthReportService healthReports;

    public HealthReportController(AuthService authService, HealthReportService healthReports) {
        this.authService = authService;
        this.healthReports = healthReports;
    }

    @PostMapping
    public HealthUploadResponse upload(Authentication auth, @RequestParam("file") MultipartFile file) {
        return healthReports.upload(user(auth), file);
    }

    @GetMapping
    public List<HealthReportResponse> list(Authentication auth) {
        return healthReports.list(user(auth));
    }

    @GetMapping("/status")
    public HealthStatusResponse status(Authentication auth) {
        return healthReports.status(user(auth));
    }

    @GetMapping("/{id}")
    public HealthReportResponse get(Authentication auth, @PathVariable Long id) {
        return healthReports.get(user(auth), id);
    }

    private User user(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return authService.requireUser(auth.getName());
    }
}
