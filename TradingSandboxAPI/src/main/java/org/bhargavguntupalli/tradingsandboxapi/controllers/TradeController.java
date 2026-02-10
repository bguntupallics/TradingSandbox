package org.bhargavguntupalli.tradingsandboxapi.controllers;

import jakarta.validation.Valid;
import org.bhargavguntupalli.tradingsandboxapi.dto.PortfolioDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeHistoryDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeRequestDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeResultDto;
import org.bhargavguntupalli.tradingsandboxapi.services.TradingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final TradingService tradingService;

    public TradeController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/execute")
    public ResponseEntity<TradeResultDto> executeTrade(
            Authentication auth,
            @Valid @RequestBody TradeRequestDto request) {
        return ResponseEntity.ok(tradingService.executeTrade(auth.getName(), request));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioDto> getPortfolio(Authentication auth) {
        return ResponseEntity.ok(tradingService.getPortfolio(auth.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TradeHistoryDto>> getTradeHistory(Authentication auth) {
        return ResponseEntity.ok(tradingService.getTradeHistory(auth.getName()));
    }
}
