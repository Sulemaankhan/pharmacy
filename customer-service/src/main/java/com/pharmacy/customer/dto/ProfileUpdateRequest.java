package com.pharmacy.customer.dto;

public record ProfileUpdateRequest(
        String name,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String pincode
) {}
