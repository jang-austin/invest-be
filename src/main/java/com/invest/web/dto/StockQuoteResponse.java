package com.invest.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuoteResponse(String symbol, BigDecimal price, String name, Instant lastUpdated) {}
