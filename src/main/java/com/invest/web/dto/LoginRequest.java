package com.invest.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String userId) {}
