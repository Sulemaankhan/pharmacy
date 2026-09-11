package com.pharmacy.drugstore.dto;

public record PaymentRequest(
        String paymentMode,
        String upiId,
        String cardNumber,
        String cardHolder,
        String expiry,
        String cvv,
        String bankName,
        String walletProvider,
        String walletPhone,
        String recipientName,
        String address,
        String city,
        String state,
        String pincode,
        String contactNumber,
        String email
) {}
