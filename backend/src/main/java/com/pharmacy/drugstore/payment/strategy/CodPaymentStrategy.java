package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CodPaymentStrategy implements PaymentStrategy {
    private static final BigDecimal COD_LIMIT = new BigDecimal("5000");

    @Override
    public PaymentMode mode() {
        return PaymentMode.COD;
    }

    @Override
    public PaymentOutcome process(PaymentContext context) {
        if (context.amount().compareTo(COD_LIMIT) > 0) {
            return PaymentOutcome.failed("Cash on delivery is available only for orders up to ₹5,000");
        }
        return PaymentOutcome.success("COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Order confirmed. Pay in cash when delivered", "Cash on delivery");
    }
}
