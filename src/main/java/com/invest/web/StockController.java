package com.invest.web;

import com.invest.service.stock.StockPriceRegistry;
import com.invest.service.stock.YahooChartClient;
import com.invest.service.stock.YahooSearchClient;
import com.invest.web.dto.HistoryPoint;
import com.invest.web.dto.StockQuoteResponse;
import com.invest.web.dto.StockSearchResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceRegistry stockPriceRegistry;
    private final YahooSearchClient yahooSearchClient;
    private final YahooChartClient yahooChartClient;

    public StockController(StockPriceRegistry stockPriceRegistry, YahooSearchClient yahooSearchClient, YahooChartClient yahooChartClient) {
        this.stockPriceRegistry = stockPriceRegistry;
        this.yahooSearchClient = yahooSearchClient;
        this.yahooChartClient = yahooChartClient;
    }

    @GetMapping("/{symbol}/quote")
    public StockQuoteResponse quote(@PathVariable String symbol) {
        stockPriceRegistry.watch(symbol);
        BigDecimal price = stockPriceRegistry.getOrThrow(symbol);
        String name = stockPriceRegistry.getName(symbol);
        return new StockQuoteResponse(
                symbol.trim().toUpperCase(),
                price,
                name,
                stockPriceRegistry.getLastUpdated(symbol),
                stockPriceRegistry.getPreMarketPrice(symbol),
                stockPriceRegistry.getPostMarketPrice(symbol),
                stockPriceRegistry.getMarketState(symbol),
                stockPriceRegistry.getCurrency(symbol),
                stockPriceRegistry.getRegularMarketChange(symbol),
                stockPriceRegistry.getRegularMarketChangePercent(symbol));
    }

    @GetMapping("/search")
    public List<StockSearchResult> search(@RequestParam String q) {
        if (q == null || q.isBlank()) return List.of();
        return yahooSearchClient.search(q.trim()).stream()
                .map(r -> {
                    String sym = r.symbol();
                    // 검색 API가 가격 데이터를 주지 않으면 레지스트리 캐시로 보완
                    BigDecimal price = r.regularMarketPrice() != null
                            ? r.regularMarketPrice() : stockPriceRegistry.getCached(sym);
                    BigDecimal change = r.regularMarketChange() != null
                            ? r.regularMarketChange() : stockPriceRegistry.getRegularMarketChange(sym);
                    BigDecimal changePct = r.regularMarketChangePercent() != null
                            ? r.regularMarketChangePercent() : stockPriceRegistry.getRegularMarketChangePercent(sym);
                    String currency = r.currency() != null
                            ? r.currency() : stockPriceRegistry.getCurrency(sym);
                    return new StockSearchResult(sym, r.name(), r.exchange(), r.type(),
                            price, change, changePct, currency);
                })
                .toList();
    }

    @GetMapping("/{symbol}/history")
    public List<HistoryPoint> history(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1mo") String range) {
        return yahooChartClient.fetchHistory(symbol.trim().toUpperCase(), range);
    }
}
