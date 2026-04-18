package com.invest.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MoneyRequest(@NotBlank String userId, @NotNull @Positive BigDecimal amount) {}
