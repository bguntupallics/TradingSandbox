package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.dto.*;
import org.bhargavguntupalli.tradingsandboxapi.models.*;
import org.bhargavguntupalli.tradingsandboxapi.repositories.HoldingRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.TradeRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.DailyPriceService;
import org.bhargavguntupalli.tradingsandboxapi.services.TradingService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradingServiceImpl implements TradingService {

    private final UserRepository userRepo;
    private final HoldingRepository holdingRepo;
    private final TradeRepository tradeRepo;
    private final DailyPriceService dailyPriceService;

    public TradingServiceImpl(UserRepository userRepo,
                              HoldingRepository holdingRepo,
                              TradeRepository tradeRepo,
                              DailyPriceService dailyPriceService) {
        this.userRepo = userRepo;
        this.holdingRepo = holdingRepo;
        this.tradeRepo = tradeRepo;
        this.dailyPriceService = dailyPriceService;
    }

    @Override
    @Transactional
    public TradeResultDto executeTrade(String username, TradeRequestDto request) {
        // 1) Check market status
        MarketStatusDto marketStatus = dailyPriceService.fetchMarketStatus();
        if (!marketStatus.isOpen()) {
            throw new IllegalStateException("Market is currently closed. Trading is only available during market hours.");
        }

        // 2) Get current price
        String symbol = request.getSymbol().toUpperCase().trim();
        TradeResponseDto latestTrade = dailyPriceService.getLatestTrade(symbol);
        if (latestTrade == null) {
            throw new IllegalStateException("Unable to fetch current price for " + symbol + ". Please try again.");
        }

        BigDecimal price = BigDecimal.valueOf(latestTrade.getPrice()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal quantity = request.getQuantity();
        BigDecimal totalCost = price.multiply(quantity).setScale(4, RoundingMode.HALF_UP);

        // 3) Get user
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // 4) Execute based on trade type
        if (request.getType() == TradeType.BUY) {
            executeBuy(user, symbol, quantity, price, totalCost);
        } else {
            executeSell(user, symbol, quantity, price, totalCost);
        }

        // 5) Log the trade
        Trade trade = new Trade();
        trade.setUser(user);
        trade.setSymbol(symbol);
        trade.setType(request.getType());
        trade.setQuantity(quantity);
        trade.setPricePerShare(price);
        trade.setTotalCost(totalCost);
        trade.setExecutedAt(LocalDateTime.now());
        tradeRepo.save(trade);

        // 6) Build response
        TradeResultDto result = new TradeResultDto();
        result.setTradeId(trade.getId());
        result.setSymbol(symbol);
        result.setType(request.getType().name());
        result.setQuantity(quantity);
        result.setPricePerShare(price);
        result.setTotalCost(totalCost);
        result.setRemainingCashBalance(user.getCashBalance());
        result.setExecutedAt(trade.getExecutedAt());
        return result;
    }

    private void executeBuy(User user, String symbol, BigDecimal quantity,
                            BigDecimal price, BigDecimal totalCost) {
        // Check sufficient funds
        if (user.getCashBalance().compareTo(totalCost) < 0) {
            throw new IllegalArgumentException("Insufficient funds. Required: $"
                    + totalCost.setScale(2, RoundingMode.HALF_UP)
                    + ", Available: $" + user.getCashBalance().setScale(2, RoundingMode.HALF_UP));
        }

        // Debit cash
        user.setCashBalance(user.getCashBalance().subtract(totalCost));
        userRepo.save(user);

        // Upsert holding
        Holding holding = holdingRepo.findByUserAndSymbol(user, symbol).orElse(null);
        if (holding != null) {
            // Weighted average cost
            BigDecimal oldTotal = holding.getQuantity().multiply(holding.getAverageCost());
            BigDecimal newTotal = quantity.multiply(price);
            BigDecimal combinedQty = holding.getQuantity().add(quantity);
            BigDecimal newAvgCost = oldTotal.add(newTotal)
                    .divide(combinedQty, 4, RoundingMode.HALF_UP);
            holding.setQuantity(combinedQty);
            holding.setAverageCost(newAvgCost);
        } else {
            holding = new Holding();
            holding.setUser(user);
            holding.setSymbol(symbol);
            holding.setQuantity(quantity);
            holding.setAverageCost(price);
        }
        holdingRepo.save(holding);
    }

    private void executeSell(User user, String symbol, BigDecimal quantity,
                             BigDecimal price, BigDecimal totalCost) {
        // Check holding exists with sufficient shares
        Holding holding = holdingRepo.findByUserAndSymbol(user, symbol)
                .orElseThrow(() -> new IllegalArgumentException("You don't own any shares of " + symbol));

        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient shares. You own "
                    + holding.getQuantity().setScale(2, RoundingMode.HALF_UP)
                    + " shares of " + symbol);
        }

        // Credit cash
        user.setCashBalance(user.getCashBalance().add(totalCost));
        userRepo.save(user);

        // Update or delete holding
        BigDecimal remaining = holding.getQuantity().subtract(quantity);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            holdingRepo.delete(holding);
        } else {
            holding.setQuantity(remaining);
            holdingRepo.save(holding);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioDto getPortfolio(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        List<Holding> holdings = holdingRepo.findByUser(user);
        BigDecimal holdingsValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        List<HoldingDto> holdingDtos = new ArrayList<>();

        for (Holding h : holdings) {
            HoldingDto dto = new HoldingDto();
            dto.setSymbol(h.getSymbol());
            dto.setQuantity(h.getQuantity());
            dto.setAverageCost(h.getAverageCost());

            // Fetch current price
            BigDecimal currentPrice;
            TradeResponseDto trade = dailyPriceService.getLatestTrade(h.getSymbol());
            if (trade != null) {
                currentPrice = BigDecimal.valueOf(trade.getPrice()).setScale(4, RoundingMode.HALF_UP);
            } else {
                // Fallback to average cost if price unavailable
                currentPrice = h.getAverageCost();
            }

            dto.setCurrentPrice(currentPrice);
            BigDecimal marketValue = h.getQuantity().multiply(currentPrice).setScale(4, RoundingMode.HALF_UP);
            dto.setMarketValue(marketValue);

            BigDecimal costBasis = h.getQuantity().multiply(h.getAverageCost()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal gainLoss = marketValue.subtract(costBasis);
            dto.setTotalGainLoss(gainLoss);

            BigDecimal gainLossPercent = costBasis.compareTo(BigDecimal.ZERO) != 0
                    ? gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            dto.setTotalGainLossPercent(gainLossPercent);

            holdingsValue = holdingsValue.add(marketValue);
            totalCostBasis = totalCostBasis.add(costBasis);
            holdingDtos.add(dto);
        }

        PortfolioDto portfolio = new PortfolioDto();
        portfolio.setCashBalance(user.getCashBalance());
        portfolio.setHoldingsValue(holdingsValue);
        portfolio.setTotalPortfolioValue(user.getCashBalance().add(holdingsValue));
        portfolio.setTotalGainLoss(holdingsValue.subtract(totalCostBasis));
        portfolio.setHoldings(holdingDtos);
        return portfolio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeHistoryDto> getTradeHistory(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return tradeRepo.findByUserOrderByExecutedAtDesc(user).stream()
                .map(t -> {
                    TradeHistoryDto dto = new TradeHistoryDto();
                    dto.setId(t.getId());
                    dto.setSymbol(t.getSymbol());
                    dto.setType(t.getType().name());
                    dto.setQuantity(t.getQuantity());
                    dto.setPricePerShare(t.getPricePerShare());
                    dto.setTotalCost(t.getTotalCost());
                    dto.setExecutedAt(t.getExecutedAt());
                    return dto;
                })
                .toList();
    }
}
