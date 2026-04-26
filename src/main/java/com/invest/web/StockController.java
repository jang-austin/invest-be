package com.invest.web;

import com.invest.service.stock.StockPriceRegistry;
import com.invest.service.stock.YahooSearchClient;
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

    public StockController(StockPriceRegistry stockPriceRegistry, YahooSearchClient yahooSearchClient) {
        this.stockPriceRegistry = stockPriceRegistry;
        this.yahooSearchClient = yahooSearchClient;
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
                stockPriceRegistry.getMarketState(symbol));
    }

    @GetMapping("/search")
    public List<StockSearchResult> search(@RequestParam String q) {
        if (q == null || q.isBlank()) return List.of();
        return yahooSearchClient.search(q.trim());
    }
}
