package com.invest.web;

import com.invest.service.stock.StockPriceRegistry;
import com.invest.web.dto.StockQuoteResponse;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceRegistry stockPriceRegistry;

    public StockController(StockPriceRegistry stockPriceRegistry) {
        this.stockPriceRegistry = stockPriceRegistry;
    }

    @GetMapping("/{symbol}/quote")
    public StockQuoteResponse quote(@PathVariable String symbol) {
        stockPriceRegistry.watch(symbol);
        BigDecimal price = stockPriceRegistry.getOrThrow(symbol);
        return new StockQuoteResponse(symbol.trim().toUpperCase(), price, stockPriceRegistry.getLastUpdated(symbol));
    }
}
