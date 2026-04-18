package com.invest.service;

import com.invest.domain.Holding;
import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.repo.HoldingRepository;
import com.invest.repo.LedgerEntryRepository;
import com.invest.repo.UserRepository;
import com.invest.service.stock.StockPriceRegistry;
import com.invest.web.dto.HoldingInfo;
import com.invest.web.dto.PortfolioResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private static final BigDecimal FALLBACK_KRW_RATE = BigDecimal.valueOf(1500);

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

        BigDecimal krwRate = resolveKrwRate();

        BigDecimal stockValue = BigDecimal.ZERO;
        for (Holding h : holdings) {
            stockPriceRegistry.watch(h.getSymbol());
            BigDecimal px = stockPriceRegistry.getCached(h.getSymbol());
            if (px == null) {
                px = stockPriceRegistry.getOrThrow(h.getSymbol());
            }
            BigDecimal krwPx = px.multiply(krwRate).setScale(4, RoundingMode.HALF_UP);
            stockValue = stockValue.add(h.getQuantity().multiply(krwPx));
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
            pnlPercent = total.subtract(netFunding)
                    .divide(netFunding, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioResponse(cash, stockValue, total, netFunding, pnlPercent);
    }

    @Transactional(readOnly = true)
    public List<HoldingInfo> getHoldings(String userId) {
        var user = userRepository
                .findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Holding> holdings = holdingRepository.findByUserId(user.getId());
        BigDecimal krwRate = resolveKrwRate();

        return holdings.stream().map(h -> {
            BigDecimal qty = h.getQuantity();
            BigDecimal avgCostKrw = h.getAverageCost().setScale(4, RoundingMode.HALF_UP);

            BigDecimal currentPriceKrw;
            try {
                stockPriceRegistry.watch(h.getSymbol());
                BigDecimal usdPx = stockPriceRegistry.getCached(h.getSymbol());
                if (usdPx == null) usdPx = stockPriceRegistry.getOrThrow(h.getSymbol());
                currentPriceKrw = usdPx.multiply(krwRate).setScale(4, RoundingMode.HALF_UP);
            } catch (Exception e) {
                currentPriceKrw = avgCostKrw;
            }

            BigDecimal currentValueKrw = currentPriceKrw.multiply(qty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal costBasisKrw = avgCostKrw.multiply(qty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal pnlAmountKrw = currentValueKrw.subtract(costBasisKrw).setScale(4, RoundingMode.HALF_UP);

            BigDecimal pnlPercent = null;
            if (costBasisKrw.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = pnlAmountKrw
                        .divide(costBasisKrw, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            return new HoldingInfo(h.getSymbol(), qty, avgCostKrw, currentPriceKrw, currentValueKrw, pnlAmountKrw, pnlPercent);
        }).collect(Collectors.toList());
    }

    private BigDecimal resolveKrwRate() {
        try {
            BigDecimal cached = stockPriceRegistry.getCached("KRW=X");
            if (cached != null && cached.compareTo(BigDecimal.ZERO) > 0) {
                return cached;
            }
            return stockPriceRegistry.getOrThrow("KRW=X");
        } catch (Exception e) {
            return FALLBACK_KRW_RATE;
        }
    }
}
