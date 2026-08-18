package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.export.ExportFile;
import com.pharmacy.drugstore.service.AuthService;
import com.pharmacy.drugstore.service.OrderExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/orders")
public class OrderExportController {
    private final OrderExportService exportService;
    private final AuthService authService;

    public OrderExportController(OrderExportService exportService, AuthService authService) {
        this.exportService = exportService;
        this.authService = authService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportHistory(
            Authentication auth,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String status) {
        try {
            return file(exportService.exportHistory(user(auth), format, status));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/{orderNumber}/export")
    public ResponseEntity<byte[]> exportOrder(
            Authentication auth,
            @PathVariable String orderNumber,
            @RequestParam(defaultValue = "pdf") String format) {
        try {
            return file(exportService.exportOrder(user(auth), orderNumber, format));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private User user(Authentication auth) {
        return authService.requireUser(auth.getName());
    }

    private ResponseEntity<byte[]> file(ExportFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
