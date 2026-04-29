package com.invest.service;

import com.invest.domain.Holding;
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
        BigDecimal krwRate = stockPriceRegistry.resolveKrwRate();

        BigDecimal stockValue = BigDecimal.ZERO;
        for (Holding h : holdings) {
            stockPriceRegistry.watch(h.getSymbol());
            stockValue = stockValue.add(
                    h.getQuantity().multiply(stockPriceRegistry.getEffectiveKrwPrice(h.getSymbol(), krwRate)));
        }
        stockValue = stockValue.setScale(4, RoundingMode.HALF_UP);

        BigDecimal cash = user.getBalance().setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = cash.add(stockValue).setScale(4, RoundingMode.HALF_UP);

        // DB 쿼리로 ADD_MONEY / SUBTRACT_MONEY만 조회
        BigDecimal netFunding = ledgerEntryRepository
                .findByUserIdAndTypeInOrderByCreatedAtDesc(
                        user.getId(), List.of(TransactionType.ADD_MONEY, TransactionType.SUBTRACT_MONEY))
                .stream()
                .map(e -> e.getCashDelta())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

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
        BigDecimal krwRate = stockPriceRegistry.resolveKrwRate();

        return holdings.stream().map(h -> {
            BigDecimal qty = h.getQuantity();
            BigDecimal avgCostKrw = h.getAverageCost().setScale(4, RoundingMode.HALF_UP);

            BigDecimal currentPriceKrw;
            try {
                stockPriceRegistry.watch(h.getSymbol());
                currentPriceKrw = stockPriceRegistry.getEffectiveKrwPrice(h.getSymbol(), krwRate);
            } catch (Exception e) {
                currentPriceKrw = avgCostKrw;
            }

            BigDecimal currentValueKrw = currentPriceKrw.multiply(qty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal costBasisKrw = avgCostKrw.multiply(qty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal pnlAmountKrw = currentValueKrw.subtract(costBasisKrw).setScale(4, RoundingMode.HALF_UP);

            BigDecimal pnlPercent = costBasisKrw.compareTo(BigDecimal.ZERO) > 0
                    ? pnlAmountKrw.divide(costBasisKrw, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : null;

            return new HoldingInfo(h.getSymbol(), qty, avgCostKrw, currentPriceKrw, currentValueKrw, pnlAmountKrw, pnlPercent);
        }).toList();
    }
}
