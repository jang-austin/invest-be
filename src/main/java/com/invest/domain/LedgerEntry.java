package com.invest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(length = 16)
    private String symbol;

    @Column(precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 8)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cashDelta;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {}

    public LedgerEntry(
            String userId,
            TransactionType type,
            String symbol,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal cashDelta,
            Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.cashDelta = cashDelta;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public TransactionType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getCashDelta() {
        return cashDelta;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
