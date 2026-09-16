package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.PasswordResetToken;
import com.pharmacy.drugstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
