package com.invest.service;

import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.repo.LedgerEntryRepository;
import com.invest.service.stock.StockPriceRegistry;
import com.invest.service.stock.YahooChartClient;
import com.invest.web.dto.HistoryPoint;
import com.invest.web.dto.WhatIfResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class WhatIfService {

    private static final BigDecimal DEFAULT_KRW_RATE = new BigDecimal("1500");

    private final LedgerEntryRepository ledgerEntryRepository;
    private final YahooChartClient yahooChartClient;
    private final StockPriceRegistry stockPriceRegistry;
    private final PortfolioService portfolioService;

    public WhatIfService(
            LedgerEntryRepository ledgerEntryRepository,
            YahooChartClient yahooChartClient,
            StockPriceRegistry stockPriceRegistry,
            PortfolioService portfolioService) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.yahooChartClient = yahooChartClient;
        this.stockPriceRegistry = stockPriceRegistry;
        this.portfolioService = portfolioService;
    }

    public WhatIfResponse calculate(String userId, String symbol) {
        String sym = symbol.trim().toUpperCase();

        // 입금 내역
        List<LedgerEntry> deposits = ledgerEntryRepository
                .findByUserIdAndTypeInOrderByCreatedAtDesc(userId, List.of(TransactionType.ADD_MONEY));
        if (deposits.isEmpty()) return zero(sym);

        BigDecimal krwRate = resolveKrwRate();

        // 비교 종목 2년 일별 히스토리 (날짜 → 종가 USD) — what-if는 일봉 정확도 필요
        List<HistoryPoint> history = yahooChartClient.fetchHistory(sym, "2y_daily");
        if (history.isEmpty()) return zero(sym);

        TreeMap<LocalDate, BigDecimal> priceMap = new TreeMap<>();
        for (HistoryPoint p : history) {
            try {
                LocalDate date = LocalDate.parse(p.date().substring(0, 10));
                priceMap.put(date, p.close());
            } catch (Exception ignored) {}
        }

        BigDecimal totalDeposited = BigDecimal.ZERO;
        BigDecimal simulatedShares = BigDecimal.ZERO;

        for (LedgerEntry entry : deposits) {
            BigDecimal depositKrw = entry.getCashDelta(); // ADD_MONEY는 양수
            if (depositKrw.compareTo(BigDecimal.ZERO) <= 0) continue;

            LocalDate depositDate = entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            BigDecimal price = findClosestPrice(priceMap, depositDate);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) continue;

            // KRW → USD → 주식 매수
            BigDecimal depositUsd = depositKrw.divide(krwRate, 8, RoundingMode.HALF_UP);
            simulatedShares = simulatedShares.add(depositUsd.divide(price, 8, RoundingMode.HALF_UP));
            totalDeposited = totalDeposited.add(depositKrw);
        }

        if (totalDeposited.compareTo(BigDecimal.ZERO) == 0) return zero(sym);

        // 현재 가치
        BigDecimal currentPrice = stockPriceRegistry.getCached(sym);
        if (currentPrice == null) {
            stockPriceRegistry.watch(sym);
            currentPrice = stockPriceRegistry.getOrThrow(sym);
        }
        BigDecimal currentValueKrw = simulatedShares.multiply(currentPrice).multiply(krwRate)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal returnAmount = currentValueKrw.subtract(totalDeposited);
        BigDecimal returnPercent = returnAmount
                .divide(totalDeposited, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // 실제 포트폴리오 수익률
        BigDecimal actualReturn = null;
        try {
            var summary = portfolioService.summarize(userId);
            actualReturn = summary.pnlPercentVsFunding() != null
                    ? summary.pnlPercentVsFunding().setScale(2, RoundingMode.HALF_UP)
                    : null;
        } catch (Exception ignored) {}

        return new WhatIfResponse(sym, totalDeposited, currentValueKrw, returnAmount, returnPercent, actualReturn);
    }

    private BigDecimal findClosestPrice(TreeMap<LocalDate, BigDecimal> priceMap, LocalDate date) {
        // 해당 날짜 또는 그 이전 가장 가까운 거래일
        var entry = priceMap.floorEntry(date);
        if (entry != null) return entry.getValue();
        // 없으면 이후 가장 가까운 날
        var ceiling = priceMap.ceilingEntry(date);
        return ceiling != null ? ceiling.getValue() : null;
    }

    private BigDecimal resolveKrwRate() {
        BigDecimal cached = stockPriceRegistry.getCached("KRW=X");
        return (cached != null && cached.compareTo(BigDecimal.valueOf(100)) > 0)
                ? cached : DEFAULT_KRW_RATE;
    }

    private WhatIfResponse zero(String sym) {
        return new WhatIfResponse(sym, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
