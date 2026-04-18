package com.invest.web;

import com.invest.service.PortfolioService;
import com.invest.web.dto.HoldingInfo;
import com.invest.web.dto.PortfolioResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioResponse get(@RequestParam String userId) {
        return portfolioService.summarize(userId);
    }

    @GetMapping("/holdings")
    public List<HoldingInfo> getHoldings(@RequestParam String userId) {
        return portfolioService.getHoldings(userId);
    }
}
