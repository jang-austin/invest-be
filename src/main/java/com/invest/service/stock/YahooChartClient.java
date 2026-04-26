package com.invest.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invest.config.InvestProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class YahooChartClient {

    private static final Logger log = LoggerFactory.getLogger(YahooChartClient.class);

    public record PriceData(
            BigDecimal price,
            String name,
            BigDecimal preMarketPrice,
            BigDecimal postMarketPrice,
            String marketState) {}

    private final RestClient yahooRestClient;
    private final ObjectMapper objectMapper;
    private final InvestProperties investProperties;

    public YahooChartClient(
            @Qualifier("yahooRestClient") RestClient yahooRestClient,
            ObjectMapper objectMapper,
            InvestProperties investProperties) {
        this.yahooRestClient = yahooRestClient;
        this.objectMapper = objectMapper;
        this.investProperties = investProperties;
    }

    public Optional<PriceData> fetchLastPrice(String symbol) {
        var yf = investProperties.getYahooFinance();
        if (!yf.isEnabled()) {
            log.debug("Yahoo Finance 비활성화됨 — 시세 스킵: {}", symbol);
            return Optional.empty();
        }
        String sym = symbol.trim().toUpperCase();
        String url = yf.buildChartUrl(sym);
        try {
            var spec = yahooRestClient.get().uri(url).header("User-Agent", yf.getUserAgent());
            if (yf.getApiKey() != null && !yf.getApiKey().isBlank() && yf.getApiKeyHeader() != null && !yf.getApiKeyHeader().isBlank()) {
                spec = spec.header(yf.getApiKeyHeader(), yf.getApiKey());
            }
            if (yf.getReferer() != null && !yf.getReferer().isBlank()) {
                spec = spec.header("Referer", yf.getReferer());
            }
            String body = spec.retrieve().body(String.class);
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return Optional.empty();

            JsonNode meta = result.get(0).path("meta");

            String name = null;
            if (meta.has("longName") && !meta.get("longName").isNull()) {
                name = meta.get("longName").asText(null);
            } else if (meta.has("shortName") && !meta.get("shortName").isNull()) {
                name = meta.get("shortName").asText(null);
            }

            BigDecimal price = null;
            if (meta.has("regularMarketPrice") && meta.get("regularMarketPrice").isNumber()) {
                price = meta.get("regularMarketPrice").decimalValue();
            }
            if (price == null) {
                JsonNode indicators = result.get(0).path("indicators").path("quote");
                if (indicators.isArray() && !indicators.isEmpty()) {
                    JsonNode close = indicators.get(0).path("close");
                    if (close.isArray()) {
                        for (int i = close.size() - 1; i >= 0; i--) {
                            JsonNode v = close.get(i);
                            if (v != null && v.isNumber()) {
                                price = v.decimalValue();
                                break;
                            }
                        }
                    }
                }
            }

            BigDecimal preMarketPrice = null;
            if (meta.has("preMarketPrice") && meta.get("preMarketPrice").isNumber()) {
                preMarketPrice = meta.get("preMarketPrice").decimalValue();
            }

            BigDecimal postMarketPrice = null;
            if (meta.has("postMarketPrice") && meta.get("postMarketPrice").isNumber()) {
                postMarketPrice = meta.get("postMarketPrice").decimalValue();
            }

            String marketState = meta.has("marketState") ? meta.get("marketState").asText(null) : null;

            return price != null
                    ? Optional.of(new PriceData(price, name, preMarketPrice, postMarketPrice, marketState))
                    : Optional.empty();

        } catch (RestClientException e) {
            log.warn("Yahoo 요청 실패 {}: {}", sym, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Yahoo 응답 파싱 실패 {}: {}", sym, e.getMessage());
            return Optional.empty();
        }
    }

    /** 최근 2년간 배당 이력 반환. key = ex-dividend date, value = 주당 배당금(USD) */
    public Map<LocalDate, BigDecimal> fetchDividends(String symbol) {
        var yf = investProperties.getYahooFinance();
        if (!yf.isEnabled()) return Map.of();

        String sym = symbol.trim().toUpperCase();
        String url = yf.buildDividendUrl(sym);
        try {
            var spec = yahooRestClient.get().uri(url).header("User-Agent", yf.getUserAgent());
            if (yf.getApiKey() != null && !yf.getApiKey().isBlank()
                    && yf.getApiKeyHeader() != null && !yf.getApiKeyHeader().isBlank()) {
                spec = spec.header(yf.getApiKeyHeader(), yf.getApiKey());
            }
            if (yf.getReferer() != null && !yf.getReferer().isBlank()) {
                spec = spec.header("Referer", yf.getReferer());
            }

            String body = spec.retrieve().body(String.class);
            if (body == null || body.isBlank()) return Map.of();

            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return Map.of();

            JsonNode dividends = result.get(0).path("events").path("dividends");
            if (dividends.isMissingNode() || dividends.isNull() || !dividends.isObject()) return Map.of();

            Map<LocalDate, BigDecimal> out = new LinkedHashMap<>();
            dividends.fields().forEachRemaining(entry -> {
                JsonNode div = entry.getValue();
                long epochSecs = div.path("date").asLong(0);
                JsonNode amountNode = div.path("amount");
                if (epochSecs > 0 && amountNode.isNumber()) {
                    BigDecimal amount = amountNode.decimalValue();
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        LocalDate date = Instant.ofEpochSecond(epochSecs)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate();
                        out.put(date, amount);
                    }
                }
            });
            return out;

        } catch (RestClientException e) {
            log.warn("Yahoo 배당 요청 실패 {}: {}", sym, e.getMessage());
            return Map.of();
        } catch (Exception e) {
            log.warn("Yahoo 배당 파싱 실패 {}: {}", sym, e.getMessage());
            return Map.of();
        }
    }
}
