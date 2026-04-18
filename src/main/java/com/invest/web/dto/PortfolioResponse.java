package com.invest.web.dto;

import java.math.BigDecimal;

public record PortfolioResponse(
        BigDecimal cashBalance,
        BigDecimal stockValue,
        BigDecimal totalValue,
        BigDecimal netManualFunding,
        BigDecimal pnlPercentVsFunding) {}
