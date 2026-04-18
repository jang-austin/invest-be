package com.invest.service;

import com.invest.domain.Holding;
import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.domain.User;
import com.invest.repo.HoldingRepository;
import com.invest.repo.LedgerEntryRepository;
import com.invest.repo.UserRepository;
import com.invest.service.stock.StockPriceRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradingService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StockPriceRegistry stockPriceRegistry;

    public TradingService(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            LedgerEntryRepository ledgerEntryRepository,
            StockPriceRegistry stockPriceRegistry) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stockPriceRegistry = stockPriceRegistry;
    }

    @Transactional
    public User buy(String userId, String symbol, BigDecimal quantity, BigDecimal exchangeRate) {
        String sym = symbol.trim().toUpperCase();
        BigDecimal qty = quantity.setScale(8, RoundingMode.HALF_UP);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        BigDecimal usdPrice = stockPriceRegistry.getOrThrow(sym);
        BigDecimal krwPrice = usdPrice.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal cost = krwPrice.multiply(qty).setScale(4, RoundingMode.HALF_UP);

        User user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (user.getBalance().compareTo(cost) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        Holding holding = holdingRepository
                .findByUserIdAndSymbol(user.getId(), sym)
                .orElse(new Holding(user.getId(), sym, BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimal oldQty = holding.getQuantity();
        BigDecimal oldAvg = holding.getAverageCost();
        BigDecimal newQty = oldQty.add(qty);
        BigDecimal newAvg;
        if (oldQty.compareTo(BigDecimal.ZERO) == 0) {
            newAvg = krwPrice;
        } else {
            newAvg = oldQty
                    .multiply(oldAvg)
                    .add(qty.multiply(krwPrice))
                    .divide(newQty, 8, RoundingMode.HALF_UP);
        }
        holding.setQuantity(newQty);
        holding.setAverageCost(newAvg);
        holdingRepository.save(holding);

        user.setBalance(user.getBalance().subtract(cost));
        userRepository.save(user);

        ledgerEntryRepository.save(new LedgerEntry(
                user.getId(), TransactionType.BUY, sym, qty, krwPrice, cost.negate(), Instant.now()));
        return user;
    }

    @Transactional
    public User sell(String userId, String symbol, BigDecimal quantity, BigDecimal exchangeRate) {
        String sym = symbol.trim().toUpperCase();
        BigDecimal qty = quantity.setScale(8, RoundingMode.HALF_UP);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        BigDecimal usdPrice = stockPriceRegistry.getOrThrow(sym);
        BigDecimal krwPrice = usdPrice.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);

        User user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Holding holding = holdingRepository
                .findByUserIdAndSymbol(user.getId(), sym)
                .orElseThrow(() -> new IllegalStateException("보유 수량이 없습니다."));
        if (holding.getQuantity().compareTo(qty) < 0) {
            throw new IllegalStateException("보유 수량이 부족합니다.");
        }

        BigDecimal proceeds = krwPrice.multiply(qty).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newQty = holding.getQuantity().subtract(qty);
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(newQty);
            holdingRepository.save(holding);
        }

        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);

        ledgerEntryRepository.save(new LedgerEntry(
                user.getId(), TransactionType.SELL, sym, qty, krwPrice, proceeds, Instant.now()));
        return user;
    }
}
