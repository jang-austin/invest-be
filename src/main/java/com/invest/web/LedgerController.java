package com.invest.web;

import com.invest.domain.TransactionType;
import com.invest.service.LedgerService;
import com.invest.web.dto.LedgerEntryResponse;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public List<LedgerEntryResponse> list(
            @RequestParam String userId, @RequestParam(required = false) List<String> types) {
        Set<TransactionType> parsed = LedgerService.parseTypes(types);
        return ledgerService.listFiltered(userId, parsed);
    }
}
