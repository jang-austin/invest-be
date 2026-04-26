package com.invest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "dividend_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol", "ex_date"}))
public class DividendRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "ex_date", nullable = false)
    private LocalDate exDate;

    /** 주당 배당금 (USD) */
    @Column(name = "dividend_per_share", nullable = false, precision = 19, scale = 8)
    private BigDecimal dividendPerShare;

    /** 처리 시점 보유 수량 */
    @Column(name = "shares_held", nullable = false, precision = 19, scale = 8)
    private BigDecimal sharesHeld;

    /** 재투자로 추가된 주식 수량 */
    @Column(name = "reinvested_shares", nullable = false, precision = 19, scale = 8)
    private BigDecimal reinvestedShares;

    /** 재투자 시점 주가 (KRW) */
    @Column(name = "price_krw", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceKrw;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected DividendRecord() {}

    public DividendRecord(
            String userId,
            String symbol,
            LocalDate exDate,
            BigDecimal dividendPerShare,
            BigDecimal sharesHeld,
            BigDecimal reinvestedShares,
            BigDecimal priceKrw,
            Instant processedAt) {
        this.userId = userId;
        this.symbol = symbol;
        this.exDate = exDate;
        this.dividendPerShare = dividendPerShare;
        this.sharesHeld = sharesHeld;
        this.reinvestedShares = reinvestedShares;
        this.priceKrw = priceKrw;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public LocalDate getExDate() { return exDate; }
    public BigDecimal getDividendPerShare() { return dividendPerShare; }
    public BigDecimal getSharesHeld() { return sharesHeld; }
    public BigDecimal getReinvestedShares() { return reinvestedShares; }
    public BigDecimal getPriceKrw() { return priceKrw; }
    public Instant getProcessedAt() { return processedAt; }
}
