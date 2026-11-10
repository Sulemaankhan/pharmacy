package com.pharmacy.drugstore.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CheckoutSagaRepository extends JpaRepository<CheckoutSaga, Long> {
    Optional<CheckoutSaga> findByOrderNumber(String orderNumber);
    List<CheckoutSaga> findByStatusInAndUpdatedAtBefore(List<String> statuses, Instant updatedBefore);
}
