package org.bhargavguntupalli.tradingsandboxapi.service;

import org.bhargavguntupalli.tradingsandboxapi.dto.*;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.bhargavguntupalli.tradingsandboxapi.repositories.DailyPriceRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.impl.DailyPriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPriceServiceExtendedTest {

    @Mock DailyPriceRepository repo;
    @Mock RestTemplate rest;

    @InjectMocks
    DailyPriceServiceImpl svc;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(svc, "fastApiBaseUrl", "http://fake-api");
        ReflectionTestUtils.setField(svc, "fastApiAccessKey", "FAKEKEY");
    }

    // ── save ─────────────────────────────────────────────────────────────

    @Test
    void save_PersistsAndReturnsDto() {
        DailyPriceDto dto = new DailyPriceDto("AAPL", LocalDate.of(2025, 7, 9), BigDecimal.valueOf(150));

        DailyPriceDto result = svc.save(dto);

        verify(repo).save(any(DailyPrice.class));
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    // ── findBySymbol ─────────────────────────────────────────────────────

    @Test
    void findBySymbol_ReturnsList() {
        List<DailyPrice> entities = List.of(
                new DailyPrice(new DailyPriceId("AAPL", LocalDate.of(2025, 7, 8)), BigDecimal.valueOf(148)),
                new DailyPrice(new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9)), BigDecimal.valueOf(150))
        );
        when(repo.findByIdSymbolOrderByIdDateAsc("AAPL")).thenReturn(entities);

        List<DailyPriceDto> result = svc.findBySymbol("AAPL");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2025, 7, 8));
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2025, 7, 9));
    }

    @Test
    void findBySymbol_EmptyList() {
        when(repo.findByIdSymbolOrderByIdDateAsc("UNKNOWN")).thenReturn(Collections.emptyList());

        List<DailyPriceDto> result = svc.findBySymbol("UNKNOWN");

        assertThat(result).isEmpty();
    }

    // ── findOne error cases ──────────────────────────────────────────────

    @Test
    void findOne_NullResponseBody_Throws() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);
        when(repo.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(null, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.findOne("AAPL", date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No response from bars API");
    }

    @Test
    void findOne_EmptyBars_Throws() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);
        when(repo.findById(id)).thenReturn(Optional.empty());

        BarDataDto body = new BarDataDto("AAPL", date.atStartOfDay(), date.plusDays(1).atStartOfDay(), "1Day",
                Map.of("AAPL", Collections.emptyList()));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.findOne("AAPL", date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No bar data for AAPL");
    }

    @Test
    void findOne_NoBarsForSymbol_Throws() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);
        when(repo.findById(id)).thenReturn(Optional.empty());

        // Bars map doesn't contain the symbol
        BarDataDto body = new BarDataDto("AAPL", date.atStartOfDay(), date.plusDays(1).atStartOfDay(), "1Day",
                Map.of("GOOG", List.of()));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.findOne("AAPL", date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No bar data for AAPL");
    }

    @Test
    void findOne_NoMatchingDate_Throws() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);
        when(repo.findById(id)).thenReturn(Optional.empty());

        BarDto bar = new BarDto();
        bar.setTimestamp(OffsetDateTime.parse("2025-07-08T00:00:00Z").toInstant()); // different date
        bar.setClose(150.0);

        BarDataDto body = new BarDataDto("AAPL", date.atStartOfDay(), date.plusDays(1).atStartOfDay(), "1Day",
                Map.of("AAPL", List.of(bar)));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.findOne("AAPL", date))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No bar for date");
    }

    // ── findRange ────────────────────────────────────────────────────────

    @Test
    void findRange_AllCached_NoApiCall() {
        LocalDate start = LocalDate.of(2025, 7, 8);
        LocalDate end = LocalDate.of(2025, 7, 9);

        List<DailyPrice> cached = List.of(
                new DailyPrice(new DailyPriceId("AAPL", start), BigDecimal.valueOf(148)),
                new DailyPrice(new DailyPriceId("AAPL", end), BigDecimal.valueOf(150))
        );
        when(repo.findByIdSymbolAndIdDateBetweenOrderByIdDateAsc("AAPL", start, end)).thenReturn(cached);

        List<DailyPriceDto> result = svc.findRange("AAPL", start, end);

        assertThat(result).hasSize(2);
        verifyNoInteractions(rest);
    }

    @Test
    void findRange_NullBarData_Throws() {
        LocalDate start = LocalDate.of(2025, 7, 8);
        LocalDate end = LocalDate.of(2025, 7, 9);

        when(repo.findByIdSymbolAndIdDateBetweenOrderByIdDateAsc("AAPL", start, end))
                .thenReturn(Collections.emptyList());

        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(null, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.findRange("AAPL", start, end))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch bar data");
    }

    @Test
    void findRange_MissingDates_FetchesFromApi() {
        LocalDate start = LocalDate.of(2025, 7, 8);
        LocalDate end = LocalDate.of(2025, 7, 9);

        // Only one date cached
        List<DailyPrice> cached = List.of(
                new DailyPrice(new DailyPriceId("AAPL", start), BigDecimal.valueOf(148))
        );
        when(repo.findByIdSymbolAndIdDateBetweenOrderByIdDateAsc("AAPL", start, end)).thenReturn(cached);

        // API returns the missing date
        BarDto bar = new BarDto();
        bar.setTimestamp(OffsetDateTime.parse("2025-07-09T00:00:00Z").toInstant());
        bar.setClose(150.0);

        BarDataDto body = new BarDataDto("AAPL", start.atStartOfDay(), end.atStartOfDay(), "1Day",
                Map.of("AAPL", List.of(bar)));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        List<DailyPriceDto> result = svc.findRange("AAPL", start, end);

        assertThat(result).hasSize(2);
        // Results should be sorted by date
        assertThat(result.get(0).getDate()).isEqualTo(start);
        assertThat(result.get(1).getDate()).isEqualTo(end);
        verify(repo).saveAll(anyList());
    }

    // ── getLatestTrade ───────────────────────────────────────────────────

    @Test
    void getLatestTrade_Success_ReturnsDto() {
        TradeResponseDto trade = new TradeResponseDto();
        trade.setPrice(155.75);
        trade.setTimestamp("2025-07-10T14:30:00Z");
        trade.setVolume(12345);

        ResponseEntity<TradeResponseDto> resp = new ResponseEntity<>(trade, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TradeResponseDto.class)))
                .thenReturn(resp);

        TradeResponseDto result = svc.getLatestTrade("AAPL");

        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualTo(155.75);
    }

    @Test
    void getLatestTrade_NullBody_ReturnsNull() {
        ResponseEntity<TradeResponseDto> resp = new ResponseEntity<>(null, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TradeResponseDto.class)))
                .thenReturn(resp);

        TradeResponseDto result = svc.getLatestTrade("AAPL");

        assertThat(result).isNull();
    }

    @Test
    void getLatestTrade_Non2xxStatus_ReturnsNull() {
        ResponseEntity<TradeResponseDto> resp = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TradeResponseDto.class)))
                .thenReturn(resp);

        TradeResponseDto result = svc.getLatestTrade("AAPL");

        assertThat(result).isNull();
    }

    @Test
    void getLatestTrade_ApiException_ReturnsNull() {
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TradeResponseDto.class)))
                .thenThrow(new RestClientException("Connection refused"));

        TradeResponseDto result = svc.getLatestTrade("AAPL");

        assertThat(result).isNull();
    }

    // ── fetchMarketStatus ────────────────────────────────────────────────

    @Test
    void fetchMarketStatus_Success_ReturnsDto() {
        MarketStatusDto dto = new MarketStatusDto(true, null, null);
        ResponseEntity<MarketStatusDto> resp = new ResponseEntity<>(dto, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(MarketStatusDto.class)))
                .thenReturn(resp);

        MarketStatusDto result = svc.fetchMarketStatus();

        assertThat(result).isNotNull();
        assertThat(result.isOpen()).isTrue();
    }

    @Test
    void fetchMarketStatus_Non2xxStatus_Throws() {
        ResponseEntity<MarketStatusDto> resp = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(MarketStatusDto.class)))
                .thenReturn(resp);

        assertThatThrownBy(() -> svc.fetchMarketStatus())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch market status");
    }

    // ── findByPeriod ─────────────────────────────────────────────────────

    @Test
    void findByPeriod_ApiException_ReturnsEmptyList() {
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenThrow(new RestClientException("Connection failed"));

        List<PriceDataDto> result = svc.findByPeriod("AAPL", TimePeriod.ONE_DAY);

        assertThat(result).isEmpty();
    }

    @Test
    void findByPeriod_NullBarData_ReturnsEmptyList() {
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(null, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        List<PriceDataDto> result = svc.findByPeriod("AAPL", TimePeriod.ONE_MONTH);

        assertThat(result).isEmpty();
    }

    @Test
    void findByPeriod_NullBarsMap_ReturnsEmptyList() {
        BarDataDto body = new BarDataDto();
        body.setBars(null);
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        List<PriceDataDto> result = svc.findByPeriod("AAPL", TimePeriod.ONE_MONTH);

        assertThat(result).isEmpty();
    }

    @Test
    void findByPeriod_WithData_ReturnsSortedResults() {
        Instant t1 = Instant.parse("2025-07-08T14:00:00Z");
        Instant t2 = Instant.parse("2025-07-09T14:00:00Z");

        BarDto bar1 = new BarDto();
        bar1.setTimestamp(t2);
        bar1.setClose(151.0);

        BarDto bar2 = new BarDto();
        bar2.setTimestamp(t1);
        bar2.setClose(149.0);

        BarDataDto body = new BarDataDto();
        body.setBars(Map.of("AAPL", List.of(bar1, bar2)));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        List<PriceDataDto> result = svc.findByPeriod("AAPL", TimePeriod.ONE_MONTH);

        assertThat(result).hasSize(2);
        // Should be sorted by timestamp ascending
        assertThat(result.get(0).getTimestamp()).isEqualTo(t1);
        assertThat(result.get(1).getTimestamp()).isEqualTo(t2);
    }

    @Test
    void findByPeriod_SymbolNotInBars_ReturnsEmptyList() {
        BarDataDto body = new BarDataDto();
        body.setBars(Map.of("GOOG", List.of()));
        ResponseEntity<BarDataDto> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BarDataDto.class)))
                .thenReturn(resp);

        List<PriceDataDto> result = svc.findByPeriod("AAPL", TimePeriod.ONE_WEEK);

        assertThat(result).isEmpty();
    }
}
