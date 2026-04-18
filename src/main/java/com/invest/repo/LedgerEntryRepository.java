package com.invest.repo;

import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    List<LedgerEntry> findByUserIdAndTypeInOrderByCreatedAtDesc(String userId, Collection<TransactionType> types);
}
