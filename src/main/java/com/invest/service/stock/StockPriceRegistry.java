package com.invest.service.stock;

import com.invest.config.InvestProperties;
import com.invest.repo.HoldingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StockPriceRegistry {

    private final YahooChartClient yahooChartClient;
    private final HoldingRepository holdingRepository;
    private final InvestProperties properties;

    private final Map<String, BigDecimal> lastPrice = new ConcurrentHashMap<>();
    private final Map<String, String> lastName = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastUpdated = new ConcurrentHashMap<>();
    private final Set<String> watched = ConcurrentHashMap.newKeySet();

    public StockPriceRegistry(
            YahooChartClient yahooChartClient, HoldingRepository holdingRepository, InvestProperties properties) {
        this.yahooChartClient = yahooChartClient;
        this.holdingRepository = holdingRepository;
        this.properties = properties;
    }

    public void watch(String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            watched.add(symbol.trim().toUpperCase());
        }
    }

    public BigDecimal getOrThrow(String symbol) {
        String sym = symbol.trim().toUpperCase();
        watch(sym);
        BigDecimal px = lastPrice.get(sym);
        if (px == null) {
            yahooChartClient.fetchLastPrice(sym).ifPresent(data -> {
                lastPrice.put(sym, data.price());
                lastUpdated.put(sym, Instant.now());
                if (data.name() != null) lastName.put(sym, data.name());
            });
            px = lastPrice.get(sym);
        }
        if (px == null) {
            throw new IllegalStateException("가격을 가져올 수 없습니다: " + sym);
        }
        return px;
    }

    public BigDecimal getCached(String symbol) {
        return lastPrice.get(symbol.trim().toUpperCase());
    }

    public String getName(String symbol) {
        return lastName.get(symbol.trim().toUpperCase());
    }

    public Instant getLastUpdated(String symbol) {
        return lastUpdated.get(symbol.trim().toUpperCase());
    }

    @Scheduled(fixedDelayString = "${invest.stock.refresh-ms:15000}")
    public void refresh() {
        for (String s : holdingRepository.findDistinctActiveSymbols()) {
            watched.add(s);
        }
        for (String sym : watched) {
            yahooChartClient.fetchLastPrice(sym).ifPresent(data -> {
                lastPrice.put(sym, data.price());
                lastUpdated.put(sym, Instant.now());
                if (data.name() != null) lastName.put(sym, data.name());
            });
        }
    }
}
