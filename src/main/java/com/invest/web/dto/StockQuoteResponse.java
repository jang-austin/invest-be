package com.invest.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuoteResponse(
        String symbol,
        BigDecimal price,
        String name,
        Instant lastUpdated,
        BigDecimal preMarketPrice,
        BigDecimal postMarketPrice,
        String marketState,
        String currency,
        BigDecimal regularMarketChange,
        BigDecimal regularMarketChangePercent) {}
