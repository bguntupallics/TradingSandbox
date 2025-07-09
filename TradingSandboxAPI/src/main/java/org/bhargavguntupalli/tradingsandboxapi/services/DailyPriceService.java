package org.bhargavguntupalli.tradingsandboxapi.services;

import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.MarketStatusDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface DailyPriceService {
    DailyPriceDto save(DailyPriceDto dto);
    List<DailyPriceDto> findBySymbol(String symbol);
    DailyPriceDto findOne(String symbol, LocalDate date);
    List<DailyPriceDto> findRange(String symbol, LocalDate start, LocalDate end);
    TradeResponseDto getLatestTrade(String symbol);
    MarketStatusDto fetchMarketStatus();
}
