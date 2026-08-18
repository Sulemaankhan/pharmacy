package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

@Component
public class WalletPaymentStrategy implements PaymentStrategy {
    private static final Set<String> WALLETS = Set.of("PAYTM", "PHONEPE", "AMAZONPAY", "MOBIKWIK");

    @Override
    public PaymentMode mode() {
        return PaymentMode.WALLET;
    }

    @Override
    public PaymentOutcome process(PaymentContext context) {
        var d = context.details();
        String wallet = d == null ? null : d.walletProvider();
        String phone = d == null ? null : d.walletPhone();
        if (wallet == null || !WALLETS.contains(wallet.toUpperCase())) {
            return PaymentOutcome.failed("Choose Paytm, PhonePe, Amazon Pay or MobiKwik");
        }
        if (phone == null || !phone.matches("\\d{10}")) {
            return PaymentOutcome.failed("Enter a 10-digit mobile number linked to the wallet");
        }
        return PaymentOutcome.success("WAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Wallet debit successful", wallet.toUpperCase() + " • " + phone.replaceAll("\\d(?=\\d{4})", "*"));
    }
}
