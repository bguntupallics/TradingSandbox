package org.bhargavguntupalli.tradingsandboxapi.service;

import org.bhargavguntupalli.tradingsandboxapi.dto.BarDataDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.BarDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.StockSearchResultDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.StockSuggestionDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.StockValidationDto;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.bhargavguntupalli.tradingsandboxapi.repositories.DailyPriceRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.impl.DailyPriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPriceServiceTest {

    @Mock DailyPriceRepository repo;
    @Mock RestTemplate rest;

    @InjectMocks
    DailyPriceServiceImpl svc;

    @BeforeEach
    void setup() {
        // inject the @Value fields
        ReflectionTestUtils.setField(svc, "fastApiBaseUrl", "http://fake-api");
        ReflectionTestUtils.setField(svc, "fastApiAccessKey", "FAKEKEY");
    }

    @Test
    void findOne_WhenCached_ReturnsFromRepository() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPrice existing = new DailyPrice(
                new DailyPriceId("AAPL", date),
                BigDecimal.valueOf(150.0)
        );
        when(repo.findById(existing.getId()))
                .thenReturn(Optional.of(existing));

        DailyPriceDto dto = svc.findOne("AAPL", date);

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getDate()).isEqualTo(date);
        assertThat(dto.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(150.0));
        // no external call
        verifyNoInteractions(rest);
    }

    @Test
    void findOne_WhenNotCached_FetchesFromApiAndSaves() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("GOOG", date);
        when(repo.findById(id)).thenReturn(Optional.empty());

        // prepare fake BarDto
        BarDto bar = new BarDto();
        bar.setTimestamp(OffsetDateTime.parse("2025-07-09T00:00:00Z").toInstant());
        bar.setClose(200.5);
        Map<String, List<BarDto>> map = Map.of("GOOG", List.of(bar));
        BarDataDto body = new BarDataDto(
                "GOOG",
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(),
                "1Day",
                map
        );

        ResponseEntity<BarDataDto> apiResp = new ResponseEntity<>(body, HttpStatus.OK);

        String expectedUrl = "http://fake-api/bars/GOOG?start_date=2025-07-09&end_date=2025-07-10&timeframe=1Day";
        when(rest.exchange(
                eq(expectedUrl),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(BarDataDto.class)
        )).thenReturn(apiResp);

        DailyPriceDto result = svc.findOne("GOOG", date);

        assertThat(result.getSymbol()).isEqualTo("GOOG");
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(200.5));

        // verify save
        ArgumentCaptor<DailyPrice> captor = ArgumentCaptor.forClass(DailyPrice.class);
        verify(repo).save(captor.capture());
        DailyPrice saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(200.5));
    }

    // ── searchStocks tests ─────────────────────────────────────────────────

    @Test
    void searchStocks_WhenEmptyQuery_ReturnsEmptyResult() {
        StockSearchResultDto result = svc.searchStocks("", 10);

        assertThat(result.getSuggestions()).isEmpty();
        verifyNoInteractions(rest);
    }

    @Test
    void searchStocks_WhenNullQuery_ReturnsEmptyResult() {
        StockSearchResultDto result = svc.searchStocks(null, 10);

        assertThat(result.getSuggestions()).isEmpty();
        verifyNoInteractions(rest);
    }

    @Test
    void searchStocks_WhenApiReturnsResults_ReturnsSearchResult() {
        List<StockSuggestionDto> suggestions = List.of(
                new StockSuggestionDto("AAPL", "Apple Inc.", "NASDAQ"),
                new StockSuggestionDto("AMZN", "Amazon.com Inc.", "NASDAQ")
        );
        StockSearchResultDto apiResponse = new StockSearchResultDto(suggestions);

        ResponseEntity<StockSearchResultDto> resp = new ResponseEntity<>(apiResponse, HttpStatus.OK);
        when(rest.exchange(
                eq("http://fake-api/search/AA?limit=10"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(StockSearchResultDto.class)
        )).thenReturn(resp);

        StockSearchResultDto result = svc.searchStocks("AA", 10);

        assertThat(result.getSuggestions()).hasSize(2);
        assertThat(result.getSuggestions().get(0).getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void searchStocks_WhenApiFails_ReturnsEmptyResult() {
        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(StockSearchResultDto.class)
        )).thenThrow(new RuntimeException("API error"));

        StockSearchResultDto result = svc.searchStocks("AA", 10);

        assertThat(result.getSuggestions()).isEmpty();
    }

    // ── validateSymbol tests ───────────────────────────────────────────────

    @Test
    void validateSymbol_WhenEmptySymbol_ReturnsInvalid() {
        StockValidationDto result = svc.validateSymbol("");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError()).isEqualTo("Symbol cannot be empty");
        verifyNoInteractions(rest);
    }

    @Test
    void validateSymbol_WhenNullSymbol_ReturnsInvalid() {
        StockValidationDto result = svc.validateSymbol(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError()).isEqualTo("Symbol cannot be empty");
        verifyNoInteractions(rest);
    }

    @Test
    void validateSymbol_WhenApiReturnsValid_ReturnsValidResult() {
        StockValidationDto apiResponse = StockValidationDto.valid("AAPL", "Apple Inc.", "NASDAQ");

        ResponseEntity<StockValidationDto> resp = new ResponseEntity<>(apiResponse, HttpStatus.OK);
        when(rest.exchange(
                eq("http://fake-api/validate/AAPL"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(StockValidationDto.class)
        )).thenReturn(resp);

        StockValidationDto result = svc.validateSymbol("aapl");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getName()).isEqualTo("Apple Inc.");
    }

    @Test
    void validateSymbol_WhenApiReturns404_ReturnsInvalid() {
        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(StockValidationDto.class)
        )).thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        StockValidationDto result = svc.validateSymbol("INVALID");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError()).contains("INVALID");
    }

    @Test
    void validateSymbol_WhenApiFails_ReturnsInvalid() {
        when(rest.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(StockValidationDto.class)
        )).thenThrow(new RuntimeException("Connection error"));

        StockValidationDto result = svc.validateSymbol("AAPL");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getError()).contains("try again");
    }
}
