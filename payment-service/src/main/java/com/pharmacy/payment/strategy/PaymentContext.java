package com.pharmacy.payment.strategy;


import java.math.BigDecimal;
import com.pharmacy.payment.event.PaymentCommand;

public record PaymentContext(BigDecimal amount, String orderNumber, PaymentCommand details) {}
