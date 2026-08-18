package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

@Component
public class NetBankingPaymentStrategy implements PaymentStrategy {
    private static final Set<String> BANKS = Set.of("SBI", "HDFC", "ICICI", "AXIS", "PNB", "KOTAK");

    @Override
    public PaymentMode mode() {
        return PaymentMode.NET_BANKING;
    }

    @Override
    public PaymentOutcome process(PaymentContext context) {
        String bank = context.details() == null ? null : context.details().bankName();
        if (bank == null || bank.isBlank()) {
            return PaymentOutcome.failed("Select a bank for net banking");
        }
        if (!BANKS.contains(bank.toUpperCase())) {
            return PaymentOutcome.failed("Selected bank is not supported");
        }
        return PaymentOutcome.success("NB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Net banking transfer completed via " + bank.toUpperCase(), bank.toUpperCase() + " NetBanking");
    }
}
