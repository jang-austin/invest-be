package com.invest.web.dto;

import java.math.BigDecimal;

public record StockSearchResult(
        String symbol,
        String name,
        String exchange,
        String type,
        BigDecimal regularMarketPrice,
        BigDecimal regularMarketChange,
        BigDecimal regularMarketChangePercent,
        String currency) {}
