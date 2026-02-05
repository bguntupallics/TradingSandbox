package org.bhargavguntupalli.tradingsandboxapi.controllers;

import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.MarketStatusDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.PriceDataDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TimePeriod;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeResponseDto;
import org.bhargavguntupalli.tradingsandboxapi.services.DailyPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class DailyPriceController {

    private final DailyPriceService svc;
    public DailyPriceController(DailyPriceService svc) {
        this.svc = svc;
    }

    @GetMapping("/{symbol}/{date}")
    public ResponseEntity<DailyPriceDto> getOne(@PathVariable String symbol, @PathVariable LocalDate date) {
        DailyPriceDto found = svc.findOne(symbol, date);
        return found != null ? ResponseEntity.ok(found) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{symbol}/last-week")
    public ResponseEntity<List<DailyPriceDto>> lastWeek(@PathVariable String symbol) {
        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusWeeks(1);
        return ResponseEntity.ok(svc.findRange(symbol, start, end));
    }

    @GetMapping("/{symbol}/last-month")
    public ResponseEntity<List<DailyPriceDto>> lastMonth(@PathVariable String symbol) {
        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusMonths(1);
        return ResponseEntity.ok(svc.findRange(symbol, start, end));
    }

    @GetMapping("/{symbol}/last-year")
    public ResponseEntity<List<DailyPriceDto>> lastYear(@PathVariable String symbol) {
        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusYears(1);
        return ResponseEntity.ok(svc.findRange(symbol, start, end));
    }

    @GetMapping("/{symbol}/latest-trade")
    public ResponseEntity<TradeResponseDto> latestTrade(@PathVariable String symbol) {
        TradeResponseDto trade = svc.getLatestTrade(symbol);
        if (trade == null) {
            return ResponseEntity.status(503).build(); // Service temporarily unavailable
        }
        return ResponseEntity.ok(trade);
    }

    @GetMapping("/market-status")
    public ResponseEntity<MarketStatusDto> getMarketStatus() {
        MarketStatusDto dto = svc.fetchMarketStatus();
        return ResponseEntity.ok(dto);
     }

    @GetMapping("/{symbol}/period/{period}")
    public ResponseEntity<List<PriceDataDto>> getByPeriod(
            @PathVariable String symbol,
            @PathVariable String period
    ) {
        TimePeriod timePeriod = TimePeriod.fromLabel(period);
        if (timePeriod == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(svc.findByPeriod(symbol, timePeriod));
    }
}
