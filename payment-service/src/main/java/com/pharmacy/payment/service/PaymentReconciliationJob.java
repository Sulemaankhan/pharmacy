package com.pharmacy.payment.service;

import com.pharmacy.payment.entity.PaymentIntent;
import com.pharmacy.payment.event.PaymentCommand;
import com.pharmacy.payment.repository.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PaymentReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private final PaymentIntentRepository intents;
    private final PaymentProcessingService payments;

    public PaymentReconciliationJob(PaymentIntentRepository intents, PaymentProcessingService payments) {
        this.intents = intents;
        this.payments = payments;
    }

    @Scheduled(fixedDelay = 15000)
    public void reconcileUnknown() {
        Instant cutoff = Instant.now().minus(20, ChronoUnit.SECONDS);
        List<PaymentIntent> stuck = intents.findByStatusInAndUpdatedAtBefore(List.of("PENDING", "UNKNOWN"), cutoff);
        for (PaymentIntent intent : stuck) {
            try {
                log.warn("Reconciling payment intent orderNumber={} txn={} status={}",
                        intent.getOrderNumber(), intent.getTransactionRef(), intent.getStatus());
                payments.reconcile(intent, null);
            } catch (Exception ex) {
                log.error("Payment reconciliation failed orderNumber={}", intent.getOrderNumber(), ex);
            }
        }
    }
}
