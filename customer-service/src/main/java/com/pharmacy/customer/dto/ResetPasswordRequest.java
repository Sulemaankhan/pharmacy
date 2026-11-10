package com.pharmacy.customer.dto;

public record ResetPasswordRequest(String token, String password) {}
