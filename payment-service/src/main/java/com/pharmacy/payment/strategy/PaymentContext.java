package com.pharmacy.payment.strategy;

import com.pharmacy.payment.event.PaymentCommand;
import java.math.BigDecimal;

public record PaymentContext(BigDecimal amount, String orderNumber, PaymentCommand details) {}
