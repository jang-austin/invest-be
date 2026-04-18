package com.invest.service;

import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.repo.LedgerEntryRepository;
import com.invest.web.dto.LedgerEntryResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> listFiltered(String userId, Set<TransactionType> types) {
        String id = userId.trim();
        List<LedgerEntry> rows;
        if (types == null || types.isEmpty()) {
            rows = ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(id);
        } else {
            rows = ledgerEntryRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(id, types);
        }
        return rows.stream().map(this::toResponse).toList();
    }

    private LedgerEntryResponse toResponse(LedgerEntry e) {
        return new LedgerEntryResponse(
                e.getId(),
                e.getType(),
                e.getSymbol(),
                e.getQuantity(),
                e.getUnitPrice(),
                e.getCashDelta(),
                e.getCreatedAt());
    }

    public static Set<TransactionType> parseTypes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return EnumSet.noneOf(TransactionType.class);
        }
        EnumSet<TransactionType> set = EnumSet.noneOf(TransactionType.class);
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            for (String part : s.split(",")) {
                if (part.isBlank()) {
                    continue;
                }
                set.add(TransactionType.valueOf(part.trim().toUpperCase()));
            }
        }
        return set;
    }
}
