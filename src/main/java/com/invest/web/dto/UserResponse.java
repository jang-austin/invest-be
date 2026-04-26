package com.invest.web.dto;

import java.math.BigDecimal;

public record UserResponse(String id, BigDecimal balance, String email, String name, String pictureUrl) {}
