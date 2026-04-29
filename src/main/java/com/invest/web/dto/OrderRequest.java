package com.invest.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String userId,
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal quantity) {}
