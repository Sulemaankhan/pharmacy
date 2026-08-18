package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @Query("SELECT DISTINCT t FROM PaymentTransaction t LEFT JOIN FETCH t.ledgerEntries e LEFT JOIN FETCH e.account WHERE t.transactionRef = :ref")
    Optional<PaymentTransaction> findByTransactionRef(@Param("ref") String ref);
}
