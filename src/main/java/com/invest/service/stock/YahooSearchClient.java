package com.invest.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invest.config.InvestProperties;
import com.invest.web.dto.StockSearchResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class YahooSearchClient {

    private static final Logger log = LoggerFactory.getLogger(YahooSearchClient.class);

    private final RestClient yahooRestClient;
    private final ObjectMapper objectMapper;
    private final InvestProperties investProperties;

    public YahooSearchClient(
            @Qualifier("yahooRestClient") RestClient yahooRestClient,
            ObjectMapper objectMapper,
            InvestProperties investProperties) {
        this.yahooRestClient = yahooRestClient;
        this.objectMapper = objectMapper;
        this.investProperties = investProperties;
    }

    public List<StockSearchResult> search(String query) {
        var yf = investProperties.getYahooFinance();
        if (!yf.isEnabled()) return List.of();

        String base = yf.getChartBaseUrl().replaceAll("/$", "");
        String url = UriComponentsBuilder.fromUriString(base + "/v1/finance/search")
                .queryParam("q", query)
                .queryParam("quotesCount", 10)
                .queryParam("newsCount", 0)
                .queryParam("listsCount", 0)
                .queryParam("enableFuzzyQuery", false)
                .encode()
                .build()
                .toUriString();

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
            if (body == null || body.isBlank()) return List.of();

            JsonNode root = objectMapper.readTree(body);
            JsonNode quotes = root.path("quotes");
            if (!quotes.isArray()) return List.of();

            List<StockSearchResult> results = new ArrayList<>();
            for (JsonNode q : quotes) {
                String symbol = q.path("symbol").asText(null);
                if (symbol == null || symbol.isBlank()) continue;

                String name = null;
                if (q.has("longname") && !q.get("longname").isNull()) {
                    name = q.get("longname").asText(null);
                }
                if (name == null && q.has("shortname") && !q.get("shortname").isNull()) {
                    name = q.get("shortname").asText(null);
                }

                String exchange = q.path("exchDisp").asText(null);
                if (exchange == null) exchange = q.path("exchange").asText(null);
                String type = q.path("typeDisp").asText(null);

                results.add(new StockSearchResult(symbol, name, exchange, type));
            }
            return results;

        } catch (RestClientException e) {
            log.warn("Yahoo 검색 실패 '{}': {}", query, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("Yahoo 검색 파싱 실패 '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
