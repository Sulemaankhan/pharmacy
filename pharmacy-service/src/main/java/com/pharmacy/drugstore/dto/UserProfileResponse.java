package com.pharmacy.drugstore.dto;

import java.time.Instant;

public record UserProfileResponse(
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
        Instant refreshedAt,
        String token,
        long orderCount,
        long cartCount,
        long wishlistCount,
        String lastOrderNumber
) {}
