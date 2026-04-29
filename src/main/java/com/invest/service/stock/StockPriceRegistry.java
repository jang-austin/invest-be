package com.invest.service.stock;

import com.invest.repo.HoldingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StockPriceRegistry {

    private static final BigDecimal FALLBACK_KRW_RATE = new BigDecimal("1500");

    private final YahooChartClient yahooChartClient;
    private final HoldingRepository holdingRepository;

    private final Map<String, BigDecimal> lastPrice = new ConcurrentHashMap<>();
    private final Map<String, String> lastName = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastUpdated = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastPrePrice = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastPostPrice = new ConcurrentHashMap<>();
    private final Map<String, String> lastMarketState = new ConcurrentHashMap<>();
    private final Map<String, String> lastCurrency = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastChange = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastChangePct = new ConcurrentHashMap<>();
    private final Set<String> watched = ConcurrentHashMap.newKeySet();

    public StockPriceRegistry(YahooChartClient yahooChartClient, HoldingRepository holdingRepository) {
        this.yahooChartClient = yahooChartClient;
        this.holdingRepository = holdingRepository;
    }

    /** USD/KRW 환율 조회. KRW=X 캐시 우선, 없으면 1500 폴백. 모든 서비스에서 이 메서드 사용. */
    public BigDecimal resolveKrwRate() {
        BigDecimal cached = getCached("KRW=X");
        return (cached != null && cached.compareTo(BigDecimal.valueOf(100)) > 0) ? cached : FALLBACK_KRW_RATE;
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
        if (data.currency() != null) lastCurrency.put(sym, data.currency().toUpperCase());
        else lastCurrency.remove(sym);
        if (data.regularMarketChange() != null) lastChange.put(sym, data.regularMarketChange());
        else lastChange.remove(sym);
        if (data.regularMarketChangePercent() != null) lastChangePct.put(sym, data.regularMarketChangePercent());
        else lastChangePct.remove(sym);
    }

    /**
     * marketState를 고려한 실효 가격 반환.
     * PRE/PREPRE → preMarketPrice (없으면 regularMarketPrice)
     * POST/POSTPOST/CLOSED → postMarketPrice (없으면 regularMarketPrice)
     * REGULAR → regularMarketPrice
     */
    public BigDecimal getEffectivePrice(String symbol) {
        String sym = symbol.trim().toUpperCase();
        BigDecimal reg = lastPrice.get(sym);
        if (reg == null) return null;
        String state = lastMarketState.getOrDefault(sym, "REGULAR");
        if ("PRE".equals(state) || "PREPRE".equals(state)) {
            BigDecimal pre = lastPrePrice.get(sym);
            return pre != null ? pre : reg;
        }
        if ("POST".equals(state) || "POSTPOST".equals(state) || "CLOSED".equals(state)) {
            BigDecimal post = lastPostPrice.get(sym);
            return post != null ? post : reg;
        }
        return reg;
    }

    /** 캐시된 가격을 KRW로 변환. KRW 종목은 그대로, 그 외(USD 등)는 krwRate 적용. */
    public BigDecimal toKrw(BigDecimal price, String symbol, BigDecimal krwRate) {
        String currency = lastCurrency.getOrDefault(symbol.trim().toUpperCase(), "USD");
        if ("KRW".equals(currency)) return price.setScale(4, RoundingMode.HALF_UP);
        return price.multiply(krwRate).setScale(4, RoundingMode.HALF_UP);
    }

    /** getEffectivePrice + toKrw 조합. 캐시 미스 시 Yahoo 직접 조회 후 재시도. */
    public BigDecimal getEffectiveKrwPrice(String symbol, BigDecimal krwRate) {
        String sym = symbol.trim().toUpperCase();
        BigDecimal px = getEffectivePrice(sym);
        if (px == null) {
            yahooChartClient.fetchLastPrice(sym).ifPresent(data -> store(sym, data));
            px = getEffectivePrice(sym);
        }
        if (px == null) throw new IllegalStateException("가격을 가져올 수 없습니다: " + sym);
        return toKrw(px, sym, krwRate);
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

    public String getCurrency(String symbol) {
        return lastCurrency.getOrDefault(symbol.trim().toUpperCase(), "USD");
    }

    public BigDecimal getRegularMarketChange(String symbol) {
        return lastChange.get(symbol.trim().toUpperCase());
    }

    public BigDecimal getRegularMarketChangePercent(String symbol) {
        return lastChangePct.get(symbol.trim().toUpperCase());
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
