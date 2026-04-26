package com.invest.service;

import com.invest.domain.DividendRecord;
import com.invest.domain.Holding;
import com.invest.domain.LedgerEntry;
import com.invest.domain.TransactionType;
import com.invest.repo.DividendRecordRepository;
import com.invest.repo.HoldingRepository;
import com.invest.repo.LedgerEntryRepository;
import com.invest.service.stock.StockPriceRegistry;
import com.invest.service.stock.YahooChartClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DividendService {

    private static final Logger log = LoggerFactory.getLogger(DividendService.class);
    private static final BigDecimal DEFAULT_KRW_RATE = new BigDecimal("1500");

    private final HoldingRepository holdingRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final DividendRecordRepository dividendRecordRepository;
    private final YahooChartClient yahooChartClient;
    private final StockPriceRegistry stockPriceRegistry;

    public DividendService(
            HoldingRepository holdingRepository,
            LedgerEntryRepository ledgerEntryRepository,
            DividendRecordRepository dividendRecordRepository,
            YahooChartClient yahooChartClient,
            StockPriceRegistry stockPriceRegistry) {
        this.holdingRepository = holdingRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.dividendRecordRepository = dividendRecordRepository;
        this.yahooChartClient = yahooChartClient;
        this.stockPriceRegistry = stockPriceRegistry;
    }

    @Scheduled(fixedDelay = 3_600_000) // 1시간마다
    public void processAllDividends() {
        List<String> userIds = holdingRepository.findDistinctActiveUserIds();
        for (String userId : userIds) {
            try {
                processUserDividends(userId);
            } catch (Exception e) {
                log.error("배당 처리 실패 userId={}: {}", userId, e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void processUserDividends(String userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        for (Holding holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            try {
                processHolding(userId, holding);
            } catch (Exception e) {
                log.warn("배당 처리 스킵 userId={} symbol={}: {}", userId, holding.getSymbol(), e.getMessage());
            }
        }
    }

    private void processHolding(String userId, Holding holding) {
        String symbol = holding.getSymbol();
        Map<LocalDate, BigDecimal> dividends = yahooChartClient.fetchDividends(symbol);
        if (dividends.isEmpty()) return;

        BigDecimal krwRate = resolveKrwRate();
        BigDecimal cachedPrice = stockPriceRegistry.getCached(symbol);
        if (cachedPrice == null || cachedPrice.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal priceKrw = cachedPrice.multiply(krwRate).setScale(4, RoundingMode.HALF_UP);

        for (Map.Entry<LocalDate, BigDecimal> entry : dividends.entrySet()) {
            LocalDate exDate = entry.getKey();
            BigDecimal dividendPerShare = entry.getValue(); // USD

            if (exDate.isAfter(LocalDate.now())) continue;
            if (dividendRecordRepository.existsByUserIdAndSymbolAndExDate(userId, symbol, exDate)) continue;

            BigDecimal sharesHeld = holding.getQuantity();
            BigDecimal dividendKrw = dividendPerShare
                    .multiply(krwRate)
                    .multiply(sharesHeld)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal reinvestedShares = dividendKrw.divide(priceKrw, 8, RoundingMode.HALF_UP);
            if (reinvestedShares.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 보유량 및 평단 업데이트
            BigDecimal oldQty = holding.getQuantity();
            BigDecimal oldAvg = holding.getAverageCost();
            BigDecimal newQty = oldQty.add(reinvestedShares);
            BigDecimal newAvg = oldQty.multiply(oldAvg)
                    .add(reinvestedShares.multiply(priceKrw))
                    .divide(newQty, 8, RoundingMode.HALF_UP);
            holding.setQuantity(newQty);
            holding.setAverageCost(newAvg);
            holdingRepository.save(holding);

            dividendRecordRepository.save(new DividendRecord(
                    userId, symbol, exDate,
                    dividendPerShare, sharesHeld, reinvestedShares,
                    priceKrw, Instant.now()));

            // cashDelta = 0: 배당금이 즉시 재투자되어 현금 잔고 변동 없음
            ledgerEntryRepository.save(new LedgerEntry(
                    userId, TransactionType.DIVIDEND_REINVEST,
                    symbol, reinvestedShares, priceKrw,
                    BigDecimal.ZERO, Instant.now()));

            log.info("배당 재투자 완료: userId={} symbol={} exDate={} 배당={} USD/주 → +{} 주",
                    userId, symbol, exDate, dividendPerShare, reinvestedShares.toPlainString());
        }
    }

    private BigDecimal resolveKrwRate() {
        BigDecimal cached = stockPriceRegistry.getCached("KRW=X");
        if (cached != null && cached.compareTo(BigDecimal.valueOf(100)) > 0) return cached;
        return DEFAULT_KRW_RATE;
    }
}
