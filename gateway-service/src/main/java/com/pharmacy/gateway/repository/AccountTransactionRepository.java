package com.pharmacy.gateway.repository;


import java.util.List;
import java.util.Optional;
import com.pharmacy.gateway.entity.AccountTransaction;
import com.pharmacy.gateway.entity.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    List<AccountTransaction> findByAccountOrderByCreatedAtDesc(CustomerAccount account);
    Optional<AccountTransaction> findByTransactionRefAndType(String transactionRef, String type);
    Optional<AccountTransaction> findByTransactionRefAndTypeAndAccount(
            String transactionRef, String type, CustomerAccount account);

    Optional<AccountTransaction> findFirstByTransactionRefAndTypeOrderByCreatedAtAsc(
            String transactionRef, String type);
}
