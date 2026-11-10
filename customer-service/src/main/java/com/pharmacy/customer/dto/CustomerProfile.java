package com.pharmacy.customer.dto;

import java.time.Instant;

public record CustomerProfile(
        Long userId,
        String name,
        String email,
        String role,
        String phone,
        String address,
        String city,
        String state,
        String pincode,
        Instant createdAt,
        Instant updatedAt,
        String token
) {}
