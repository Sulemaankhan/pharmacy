package com.pharmacy.payment.repository;

import com.pharmacy.payment.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);
    List<PaymentIntent> findByStatusInAndUpdatedAtBefore(List<String> statuses, Instant updatedBefore);
}
