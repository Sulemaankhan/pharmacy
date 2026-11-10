package com.pharmacy.drugstore.dto;

public record ResetPasswordRequest(String token, String password) {}
