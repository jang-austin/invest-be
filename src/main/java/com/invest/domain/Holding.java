package com.invest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "holdings")
@IdClass(Holding.HoldingId.class)
public class Holding {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Id
    @Column(length = 16)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal averageCost = BigDecimal.ZERO;

    protected Holding() {}

    public Holding(String userId, String symbol, BigDecimal quantity, BigDecimal averageCost) {
        this.userId = userId;
        this.symbol = symbol.toUpperCase();
        this.quantity = quantity;
        this.averageCost = averageCost;
    }

    public String getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }

    public static class HoldingId implements Serializable {
        private String userId;
        private String symbol;

        public HoldingId() {}

        public HoldingId(String userId, String symbol) {
            this.userId = userId;
            this.symbol = symbol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HoldingId holdingId = (HoldingId) o;
            return Objects.equals(userId, holdingId.userId) && Objects.equals(symbol, holdingId.symbol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, symbol);
        }
    }
}
