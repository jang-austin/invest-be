package com.invest.web.dto;

import java.math.BigDecimal;

public record WhatIfResponse(
        String symbol,
        BigDecimal totalDepositedKrw,
        BigDecimal currentValueKrw,
        BigDecimal returnAmountKrw,
        BigDecimal returnPercent,
        BigDecimal actualReturnPercent) {}
