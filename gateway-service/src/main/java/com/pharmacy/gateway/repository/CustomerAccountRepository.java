package com.pharmacy.gateway.repository;

import com.pharmacy.gateway.entity.CustomerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CustomerAccount a WHERE LOWER(a.email) = LOWER(:email)")
    Optional<CustomerAccount> lockByEmail(@Param("email") String email);
}
