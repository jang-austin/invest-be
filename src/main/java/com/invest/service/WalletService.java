package com.invest.service;

import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.domain.User;
import com.invest.repo.LedgerEntryRepository;
import com.invest.repo.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletService(UserRepository userRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.userRepository = userRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public User addMoney(String userId, BigDecimal amount) {
        BigDecimal a = amount.setScale(4, RoundingMode.HALF_UP);
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        User user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setBalance(user.getBalance().add(a));
        ledgerEntryRepository.save(
                new LedgerEntry(user.getId(), TransactionType.ADD_MONEY, null, null, null, a, Instant.now()));
        return userRepository.save(user);
    }

    @Transactional
    public User subtractMoney(String userId, BigDecimal amount) {
        BigDecimal a = amount.setScale(4, RoundingMode.HALF_UP);
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        User user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (user.getBalance().compareTo(a) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        user.setBalance(user.getBalance().subtract(a));
        ledgerEntryRepository.save(new LedgerEntry(
                user.getId(), TransactionType.SUBTRACT_MONEY, null, null, null, a.negate(), Instant.now()));
        return userRepository.save(user);
    }
}
