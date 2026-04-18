package com.invest.web.dto;

import com.invest.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        TransactionType type,
        String symbol,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal cashDelta,
        Instant createdAt) {}
