package com.invest.web;

import com.invest.service.TradingService;
import com.invest.web.dto.OrderRequest;
import com.invest.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final TradingService tradingService;

    public OrderController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/buy")
    public UserResponse buy(@Valid @RequestBody OrderRequest request) {
        var user = tradingService.buy(request.userId(), request.symbol(), request.quantity(), request.exchangeRate());
        return new UserResponse(user.getId(), user.getBalance());
    }

    @PostMapping("/sell")
    public UserResponse sell(@Valid @RequestBody OrderRequest request) {
        var user = tradingService.sell(request.userId(), request.symbol(), request.quantity(), request.exchangeRate());
        return new UserResponse(user.getId(), user.getBalance());
    }
}
