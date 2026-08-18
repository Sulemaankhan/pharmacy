package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.entity.User;
import java.math.BigDecimal;

public record PaymentContext(User user, BigDecimal amount, String orderNumber, PaymentRequest details) {}
