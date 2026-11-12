package com.pharmacy.drugstore.saga;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CheckoutSagaRepository extends JpaRepository<CheckoutSaga, Long> {
    Optional<CheckoutSaga> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CheckoutSaga s where s.orderNumber = :orderNumber")
    Optional<CheckoutSaga> lockByOrderNumber(@Param("orderNumber") String orderNumber);

    List<CheckoutSaga> findByStatusIn(List<String> statuses);

    List<CheckoutSaga> findByStatusInAndUpdatedAtBefore(List<String> statuses, Instant updatedBefore);
}
