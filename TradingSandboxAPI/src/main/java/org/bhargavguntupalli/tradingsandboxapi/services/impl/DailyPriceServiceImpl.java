package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.dto.BarDataDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.BarDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.TradeResponseDto;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.bhargavguntupalli.tradingsandboxapi.repositories.DailyPriceRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.DailyPriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyPriceServiceImpl implements DailyPriceService {

    private final DailyPriceRepository repo;
    private final RestTemplate rest;
    private final DailyPriceService self;

    // inject from application-dev.yml
    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${fastapi.access-key}")
    private String fastApiAccessKey;

    public DailyPriceServiceImpl(DailyPriceRepository repo, RestTemplate rest, @Lazy DailyPriceService self) {
        this.repo = repo;
        this.rest = rest;
        this.self = self;
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
        ResponseEntity<BarDataDto> resp = rest.exchange(
                url,
                HttpMethod.GET,
                entity,
                BarDataDto.class
        );
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
        DailyPriceDto dto = new DailyPriceDto(
                symbol,
                date,
                BigDecimal.valueOf(match.getClose())
        );
        save(dto);
        return dto;
    }

    @Override
    public DailyPriceDto save(DailyPriceDto dto) {
        DailyPrice entity = new DailyPrice(
                new DailyPriceId(dto.getSymbol(), dto.getDate()),
                dto.getClosingPrice()
        );
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
        List<DailyPrice> cached = repo
                .findByIdSymbolAndIdDateBetweenOrderByIdDateAsc(symbol, start, end);

        // Convert to DTOs and track which dates we have
        Set<LocalDate> haveDates = cached.stream()
                .map(e -> e.getId().getDate())
                .collect(Collectors.toSet());

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

            ResponseEntity<BarDataDto> resp = rest.exchange(
                    url, HttpMethod.GET, entity, BarDataDto.class
            );
            BarDataDto barData = resp.getBody();
            if (barData == null || barData.getBars() == null) {
                throw new RuntimeException("Failed to fetch bar data for " + symbol);
            }

            List<BarDto> bars = barData.getBars().getOrDefault(symbol, Collections.emptyList());

            // 4) For each returned bar whose date is missing, create an entity + DTO
            List<DailyPrice> toSave = new ArrayList<>();
            for (BarDto b : bars) {
                LocalDate barDate = b.getTimestamp()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();
                if (missing.contains(barDate)) {
                    DailyPriceId dpId = new DailyPriceId(symbol, barDate);
                    DailyPrice priceEntity = new DailyPrice(
                            dpId,
                            BigDecimal.valueOf(b.getClose())
                    );
                    toSave.add(priceEntity);
                    result.add(new DailyPriceDto(
                            symbol,
                            barDate,
                            priceEntity.getClosingPrice()
                    ));
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

        ResponseEntity<TradeResponseDto> resp = rest.exchange(url, HttpMethod.GET, entity, TradeResponseDto.class);

        if(!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to fetch latest trade for " + symbol + ": HTTP " + resp.getStatusCode());
        } else {
            resp.getBody();
        }

        return resp.getBody();
    }
}
