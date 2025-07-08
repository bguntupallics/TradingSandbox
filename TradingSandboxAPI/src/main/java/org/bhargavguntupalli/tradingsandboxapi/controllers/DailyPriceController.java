package org.bhargavguntupalli.tradingsandboxapi.controllers;

import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
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

    @PostMapping
    public ResponseEntity<DailyPriceDto> create(@RequestBody DailyPriceDto dto) {
        return ResponseEntity.ok(svc.save(dto));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<List<DailyPriceDto>> listBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(svc.findBySymbol(symbol));
    }

    @GetMapping("/{symbol}/{date}")
    public ResponseEntity<DailyPriceDto> getOne(@PathVariable String symbol, @PathVariable LocalDate date) {
        DailyPriceDto found = svc.findOne(symbol, date);
        return found != null
                ? ResponseEntity.ok(found)
                : ResponseEntity.notFound().build();
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

    @GetMapping("/latest-trade/{symbol}")
    public TradeResponseDto latestTrade(@PathVariable String symbol) {
        return svc.getLatestTrade(symbol);
    }
}
