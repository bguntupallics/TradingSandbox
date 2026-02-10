package org.bhargavguntupalli.tradingsandboxapi.services;

import org.bhargavguntupalli.tradingsandboxapi.dto.PortfolioDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeHistoryDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeRequestDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeResultDto;

import java.util.List;

public interface TradingService {
    TradeResultDto executeTrade(String username, TradeRequestDto request);
    PortfolioDto getPortfolio(String username);
    List<TradeHistoryDto> getTradeHistory(String username);
}
