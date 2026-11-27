package com.pharmacy.drugstore.repository;


import java.util.Optional;
import com.pharmacy.drugstore.entity.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LedgerAccount a WHERE a.code = :code")
    Optional<LedgerAccount> lockByCode(@Param("code") String code);
}
