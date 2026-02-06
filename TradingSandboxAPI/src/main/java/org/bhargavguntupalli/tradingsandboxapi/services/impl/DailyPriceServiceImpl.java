package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.dto.*;
import org.springframework.web.client.HttpClientErrorException;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.bhargavguntupalli.tradingsandboxapi.repositories.DailyPriceRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.DailyPriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyPriceServiceImpl implements DailyPriceService {

    private final DailyPriceRepository repo;
    private final RestTemplate rest;

    // inject from application-dev.yml
    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${fastapi.access-key}")
    private String fastApiAccessKey;

    public DailyPriceServiceImpl(DailyPriceRepository repo, RestTemplate rest) {
        this.repo = repo;
        this.rest = rest;
    }

    @Override
    public DailyPriceDto findOne(String symbol, LocalDate date) {
        DailyPriceId id = new DailyPriceId(symbol, date);

        // 1) check DB cache
        Optional<DailyPrice> existing = repo.findById(id);
        if (existing.isPresent()) {
            DailyPrice e = existing.get();
            return new DailyPriceDto(e.getId().getSymbol(), e.getId().getDate(), e.getClosingPrice());
        }

        // 2) build headers with X-ACCESS-KEY
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 3) call FastAPI /bars
        LocalDate start = date;
        LocalDate end   = date.plusDays(1);
        String url = String.format(
                "%s/bars/%s?start_date=%s&end_date=%s&timeframe=1Day",
                fastApiBaseUrl, symbol, start, end
        );

        ResponseEntity<BarDataDto> resp = rest.exchange(url, HttpMethod.GET, entity, BarDataDto.class);
        BarDataDto barData = resp.getBody();
        if (barData == null) {
            throw new RuntimeException("No response from bars API for " + symbol);
        }

        // 4) extract list under the symbol key
        Map<String, List<BarDto>> barsMap = barData.getBars();
        List<BarDto> bars = barsMap.get(symbol);
        if (bars == null || bars.isEmpty()) {
            throw new RuntimeException("No bar data for " + symbol + " on " + date);
        }

        // 5) find the bar whose timestamp matches our date
        BarDto match = bars.stream()
                .filter(b -> b.getTimestamp()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .equals(date))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No bar for date " + date));

        // 6) save to DB and return
        DailyPriceDto dto = new DailyPriceDto(symbol, date, BigDecimal.valueOf(match.getClose()));
        save(dto);
        return dto;
    }

    @Override
    public DailyPriceDto save(DailyPriceDto dto) {
        DailyPrice entity = new DailyPrice(new DailyPriceId(dto.getSymbol(), dto.getDate()), dto.getClosingPrice());
        repo.save(entity);
        return dto;
    }

    @Override
    public List<DailyPriceDto> findBySymbol(String symbol) {
        return repo.findByIdSymbolOrderByIdDateAsc(symbol).stream()
                .map(e -> new DailyPriceDto(
                        e.getId().getSymbol(),
                        e.getId().getDate(),
                        e.getClosingPrice()
                ))
                .toList();
    }

    @Override
    public List<DailyPriceDto> findRange(String symbol, LocalDate start, LocalDate end) {
        // 1) Bulk-load any already-cached prices
        List<DailyPrice> cached = repo.findByIdSymbolAndIdDateBetweenOrderByIdDateAsc(symbol, start, end);

        // Convert to DTOs and track which dates we have
        Set<LocalDate> haveDates = cached.stream().map(e -> e.getId().getDate()).collect(Collectors.toSet());

        List<DailyPriceDto> result = cached.stream()
                .map(e -> new DailyPriceDto(
                        e.getId().getSymbol(),
                        e.getId().getDate(),
                        e.getClosingPrice()
                ))
                .collect(Collectors.toList());

        // 2) Compute which dates are missing
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!haveDates.contains(d)) {
                missing.add(d);
            }
        }

        if (!missing.isEmpty()) {
            // 3) Fetch full range once from FastAPI
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-ACCESS-KEY", fastApiAccessKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = String.format(
                    "%s/bars/%s?start_date=%s&end_date=%s&timeframe=1Day",
                    fastApiBaseUrl, symbol, start, end
            );

            ResponseEntity<BarDataDto> resp = rest.exchange(url, HttpMethod.GET, entity, BarDataDto.class);
            BarDataDto barData = resp.getBody();
            if (barData == null || barData.getBars() == null) {
                throw new RuntimeException("Failed to fetch bar data for " + symbol);
            }

            List<BarDto> bars = barData.getBars().getOrDefault(symbol, Collections.emptyList());

            // 4) For each returned bar whose date is missing, create an entity + DTO
            List<DailyPrice> toSave = new ArrayList<>();
            for (BarDto b : bars) {
                LocalDate barDate = b.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
                if (missing.contains(barDate)) {
                    DailyPriceId dpId = new DailyPriceId(symbol, barDate);
                    DailyPrice priceEntity = new DailyPrice(dpId, BigDecimal.valueOf(b.getClose()));
                    toSave.add(priceEntity);
                    result.add(new DailyPriceDto(symbol, barDate, priceEntity.getClosingPrice()));
                }
            }

            // 5) Persist all new ones in one batch
            repo.saveAll(toSave);
        }

        // 6) Sort by date and return
        result.sort(Comparator.comparing(DailyPriceDto::getDate));
        return result;
    }

    @Override
    public TradeResponseDto getLatestTrade(String symbol) {
        String url = String.format("%s/latest-trade/%s", fastApiBaseUrl, symbol);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TradeResponseDto> resp = rest.exchange(url, HttpMethod.GET, entity, TradeResponseDto.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }

            return resp.getBody();
        } catch (Exception e) {
            // Return null on FastAPI failure (transient errors like SSL issues)
            return null;
        }
    }

    @Override
    public MarketStatusDto fetchMarketStatus() {
        String url = String.format("%s/market-status", fastApiBaseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<MarketStatusDto> resp = rest.exchange(url, HttpMethod.GET, entity, MarketStatusDto.class);

        if(!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to fetch market status: HTTP " + resp.getStatusCode());
        }

        return resp.getBody();
    }

    @Override
    public List<PriceDataDto> findByPeriod(String symbol, TimePeriod period) {
        ZoneId nyZone = ZoneId.of("America/New_York");
        LocalDateTime now = LocalDateTime.now(nyZone);
        LocalDateTime start;

        if (period == TimePeriod.ONE_DAY) {
            // For intraday, start from market open (9:30 AM) today
            start = now.toLocalDate().atTime(9, 30);
        } else {
            start = now.minusDays(period.getDaysBack());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format(
                "%s/bars/%s?start_date=%s&end_date=%s&timeframe=%s",
                fastApiBaseUrl, symbol,
                start.toLocalDate(),
                now.toLocalDate().plusDays(1),
                period.getTimeframe()
        );

        BarDataDto barData;
        try {
            ResponseEntity<BarDataDto> resp = rest.exchange(url, HttpMethod.GET, entity, BarDataDto.class);
            barData = resp.getBody();
        } catch (Exception e) {
            // Log and return empty on FastAPI failure (transient errors like SSL issues)
            return Collections.emptyList();
        }

        if (barData == null || barData.getBars() == null) {
            return Collections.emptyList();
        }

        List<BarDto> bars = barData.getBars().getOrDefault(symbol, Collections.emptyList());

        // Choose date format based on period
        DateTimeFormatter formatter;
        if (period == TimePeriod.ONE_DAY) {
            formatter = DateTimeFormatter.ofPattern("h:mm a");
        } else {
            formatter = DateTimeFormatter.ofPattern("M/d");
        }

        return bars.stream()
                .map(bar -> new PriceDataDto(
                        symbol,
                        bar.getTimestamp(),
                        formatter.format(bar.getTimestamp().atZone(nyZone)),
                        BigDecimal.valueOf(bar.getClose())
                ))
                .sorted(Comparator.comparing(PriceDataDto::getTimestamp))
                .toList();
    }

    @Override
    public StockSearchResultDto searchStocks(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new StockSearchResultDto(Collections.emptyList());
        }

        String url = String.format("%s/search/%s?limit=%d", fastApiBaseUrl, query.trim(), limit);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<StockSearchResultDto> resp = rest.exchange(
                    url, HttpMethod.GET, entity, StockSearchResultDto.class);
            return resp.getBody() != null ? resp.getBody() : new StockSearchResultDto(Collections.emptyList());
        } catch (Exception e) {
            return new StockSearchResultDto(Collections.emptyList());
        }
    }

    @Override
    public StockValidationDto validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return StockValidationDto.invalid("Symbol cannot be empty");
        }

        String url = String.format("%s/validate/%s", fastApiBaseUrl, symbol.trim().toUpperCase());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-ACCESS-KEY", fastApiAccessKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<StockValidationDto> resp = rest.exchange(
                    url, HttpMethod.GET, entity, StockValidationDto.class);
            return resp.getBody() != null ? resp.getBody() : StockValidationDto.invalid("Failed to validate symbol");
        } catch (HttpClientErrorException.NotFound e) {
            return StockValidationDto.invalid("Stock symbol '" + symbol.toUpperCase() + "' not found");
        } catch (Exception e) {
            return StockValidationDto.invalid("Failed to validate symbol. Please try again.");
        }
    }
}
