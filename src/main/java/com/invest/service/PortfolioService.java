package com.invest.service;

import com.invest.domain.Holding;
import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.repo.HoldingRepository;
import com.invest.repo.LedgerEntryRepository;
import com.invest.repo.UserRepository;
import com.invest.service.stock.StockPriceRegistry;
import com.invest.web.dto.PortfolioResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StockPriceRegistry stockPriceRegistry;

    public PortfolioService(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            LedgerEntryRepository ledgerEntryRepository,
            StockPriceRegistry stockPriceRegistry) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stockPriceRegistry = stockPriceRegistry;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse summarize(String userId) {
        var user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Holding> holdings = holdingRepository.findByUserId(user.getId());

        BigDecimal stockValue = BigDecimal.ZERO;
        for (Holding h : holdings) {
            stockPriceRegistry.watch(h.getSymbol());
            BigDecimal px = stockPriceRegistry.getCached(h.getSymbol());
            if (px == null) {
                px = stockPriceRegistry.getOrThrow(h.getSymbol());
            }
            stockValue = stockValue.add(h.getQuantity().multiply(px));
        }
        stockValue = stockValue.setScale(4, RoundingMode.HALF_UP);

        BigDecimal cash = user.getBalance().setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = cash.add(stockValue).setScale(4, RoundingMode.HALF_UP);

        BigDecimal netFunding = ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(e -> e.getType() == TransactionType.ADD_MONEY
                        || e.getType() == TransactionType.SUBTRACT_MONEY)
                .map(LedgerEntry::getCashDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        netFunding = netFunding.setScale(4, RoundingMode.HALF_UP);
        BigDecimal pnlPercent = null;
        if (netFunding.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = total.subtract(netFunding).divide(netFunding, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioResponse(cash, stockValue, total, netFunding, pnlPercent);
    }
}
