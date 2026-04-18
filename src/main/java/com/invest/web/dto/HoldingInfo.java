package com.invest.web.dto;

import java.math.BigDecimal;

public record HoldingInfo(
        String symbol,
        BigDecimal quantity,
        BigDecimal averageCostKrw,
        BigDecimal currentPriceKrw,
        BigDecimal currentValueKrw,
        BigDecimal pnlAmountKrw,
        BigDecimal pnlPercent) {}
