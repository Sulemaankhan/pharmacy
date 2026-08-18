package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
}
