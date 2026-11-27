package com.pharmacy.payment.service;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.pharmacy.payment.entity.PaymentIntent;
import com.pharmacy.payment.parallel.StructuredFanout;
import com.pharmacy.payment.repository.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        List<Long> ids = intents.findByStatusInAndUpdatedAtBefore(List.of("PENDING", "UNKNOWN"), cutoff).stream()
                .map(PaymentIntent::getId)
                .toList();
        StructuredFanout.each(ids.stream().map(id -> (Runnable) () -> reconcileOne(id)).toList());
    }

    private void reconcileOne(Long id) {
        PaymentIntent intent = null;
        try {
            intent = intents.findById(id).orElse(null);
            if (intent == null) {
                return;
            }
            String status = intent.getStatus();
            if (!"PENDING".equals(status) && !"UNKNOWN".equals(status)) {
                return;
            }
            log.warn("Reconciling payment intent orderNumber={} txn={} status={}",
                    intent.getOrderNumber(), intent.getTransactionRef(), status);
            payments.reconcile(intent, null);
        } catch (Exception ex) {
            log.error("Payment reconciliation failed orderNumber={}",
                    intent == null ? id : intent.getOrderNumber(), ex);
        }
    }
}
