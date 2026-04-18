package com.invest.web;

import com.invest.service.WalletService;
import com.invest.web.dto.MoneyRequest;
import com.invest.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public UserResponse deposit(@Valid @RequestBody MoneyRequest request) {
        var user = walletService.addMoney(request.userId(), request.amount());
        return new UserResponse(user.getId(), user.getBalance());
    }

    @PostMapping("/withdraw")
    public UserResponse withdraw(@Valid @RequestBody MoneyRequest request) {
        var user = walletService.subtractMoney(request.userId(), request.amount());
        return new UserResponse(user.getId(), user.getBalance());
    }
}
