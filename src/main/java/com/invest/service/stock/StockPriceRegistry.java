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
    private final Map<String, BigDecimal> lastPrePrice = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastPostPrice = new ConcurrentHashMap<>();
    private final Map<String, String> lastMarketState = new ConcurrentHashMap<>();
    private final Set<String> watched = ConcurrentHashMap.newKeySet();

    public StockPriceRegistry(
            YahooChartClient yahooChartClient, HoldingRepository holdingRepository, InvestProperties properties) {
        this.yahooChartClient = yahooChartClient;
        this.holdingRepository = holdingRepository;
        this.properties = properties;
    }

    private static final java.util.regex.Pattern VALID_SYMBOL =
            java.util.regex.Pattern.compile("^[A-Z0-9.=^_\\-+]{1,20}$");

    public void watch(String symbol) {
        if (symbol == null || symbol.isBlank()) return;
        String sym = symbol.trim().toUpperCase();
        if (VALID_SYMBOL.matcher(sym).matches()) {
            watched.add(sym);
        }
    }

    private void store(String sym, YahooChartClient.PriceData data) {
        lastPrice.put(sym, data.price());
        lastUpdated.put(sym, Instant.now());
        if (data.name() != null) lastName.put(sym, data.name());
        if (data.preMarketPrice() != null) lastPrePrice.put(sym, data.preMarketPrice());
        else lastPrePrice.remove(sym);
        if (data.postMarketPrice() != null) lastPostPrice.put(sym, data.postMarketPrice());
        else lastPostPrice.remove(sym);
        if (data.marketState() != null) lastMarketState.put(sym, data.marketState());
        else lastMarketState.remove(sym);
    }

    public BigDecimal getOrThrow(String symbol) {
        String sym = symbol.trim().toUpperCase();
        watch(sym);
        BigDecimal px = lastPrice.get(sym);
        if (px == null) {
            yahooChartClient.fetchLastPrice(sym).ifPresent(data -> store(sym, data));
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

    public BigDecimal getPreMarketPrice(String symbol) {
        return lastPrePrice.get(symbol.trim().toUpperCase());
    }

    public BigDecimal getPostMarketPrice(String symbol) {
        return lastPostPrice.get(symbol.trim().toUpperCase());
    }

    public String getMarketState(String symbol) {
        return lastMarketState.get(symbol.trim().toUpperCase());
    }

    @Scheduled(fixedDelayString = "${invest.stock.refresh-ms:15000}")
    public void refresh() {
        for (String s : holdingRepository.findDistinctActiveSymbols()) {
            watched.add(s);
        }
        for (String sym : watched) {
            yahooChartClient.fetchLastPrice(sym).ifPresent(data -> store(sym, data));
        }
    }
}
