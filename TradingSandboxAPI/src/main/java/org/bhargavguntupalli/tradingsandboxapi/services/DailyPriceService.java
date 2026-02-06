package org.bhargavguntupalli.tradingsandboxapi.services;

import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.MarketStatusDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.PriceDataDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.StockSearchResultDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.StockValidationDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TimePeriod;
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
    List<PriceDataDto> findByPeriod(String symbol, TimePeriod period);
    StockSearchResultDto searchStocks(String query, int limit);
    StockValidationDto validateSymbol(String symbol);
}
