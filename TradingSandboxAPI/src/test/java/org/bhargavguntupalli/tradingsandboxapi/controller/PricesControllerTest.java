package org.bhargavguntupalli.tradingsandboxapi.controller;

import org.bhargavguntupalli.tradingsandboxapi.controllers.DailyPriceController;
import org.bhargavguntupalli.tradingsandboxapi.dto.*;
import org.bhargavguntupalli.tradingsandboxapi.security.CustomUserDetailsService;
import org.bhargavguntupalli.tradingsandboxapi.security.JwtProvider;
import org.bhargavguntupalli.tradingsandboxapi.services.DailyPriceService;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailyPriceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PricesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DailyPriceService svc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ── Existing test ───────────────────────────────────────────────────

    @Test
    void getMarketStatus_Returns200AndJson() throws Exception {
        OffsetDateTime nextOpen  = OffsetDateTime.parse("2025-07-10T09:30:00-04:00");
        OffsetDateTime nextClose = OffsetDateTime.parse("2025-07-09T16:00:00-04:00");
        MarketStatusDto dto = new MarketStatusDto(true, nextOpen, nextClose);
        when(svc.fetchMarketStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/prices/market-status")
                        .header("X-ACCESS-KEY", "some-valid-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.nextOpen").value("2025-07-10T09:30:00-04:00"))
                .andExpect(jsonPath("$.nextClose").value("2025-07-09T16:00:00-04:00"));
    }

    // ── getOne ──────────────────────────────────────────────────────────

    @Test
    void getOne_Found_Returns200() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 15);
        DailyPriceDto dto = new DailyPriceDto("AAPL", date, new BigDecimal("150.25"));

        when(svc.findOne("AAPL", date)).thenReturn(dto);

        mockMvc.perform(get("/api/prices/AAPL/2025-01-15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.date").value("2025-01-15"))
                .andExpect(jsonPath("$.closingPrice").value(150.25));
    }

    @Test
    void getOne_NotFound_Returns404() throws Exception {
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(svc.findOne("AAPL", date)).thenReturn(null);

        mockMvc.perform(get("/api/prices/AAPL/2025-01-15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── lastWeek ────────────────────────────────────────────────────────

    @Test
    void lastWeek_Returns200() throws Exception {
        List<DailyPriceDto> prices = List.of(
                new DailyPriceDto("AAPL", LocalDate.now().minusDays(3), new BigDecimal("148.00")),
                new DailyPriceDto("AAPL", LocalDate.now().minusDays(2), new BigDecimal("149.50"))
        );

        when(svc.findRange(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(prices);

        mockMvc.perform(get("/api/prices/AAPL/last-week")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    // ── lastMonth ───────────────────────────────────────────────────────

    @Test
    void lastMonth_Returns200() throws Exception {
        List<DailyPriceDto> prices = List.of(
                new DailyPriceDto("GOOG", LocalDate.now().minusDays(15), new BigDecimal("2800.00"))
        );

        when(svc.findRange(eq("GOOG"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(prices);

        mockMvc.perform(get("/api/prices/GOOG/last-month")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("GOOG"));
    }

    // ── lastYear ────────────────────────────────────────────────────────

    @Test
    void lastYear_Returns200() throws Exception {
        List<DailyPriceDto> prices = List.of(
                new DailyPriceDto("MSFT", LocalDate.now().minusMonths(6), new BigDecimal("320.00")),
                new DailyPriceDto("MSFT", LocalDate.now().minusMonths(3), new BigDecimal("340.00")),
                new DailyPriceDto("MSFT", LocalDate.now().minusDays(1), new BigDecimal("360.00"))
        );

        when(svc.findRange(eq("MSFT"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(prices);

        mockMvc.perform(get("/api/prices/MSFT/last-year")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].symbol").value("MSFT"));
    }

    // ── latestTrade ─────────────────────────────────────────────────────

    @Test
    void latestTrade_Success_Returns200() throws Exception {
        TradeResponseDto trade = new TradeResponseDto();
        trade.setPrice(155.75);
        trade.setTimestamp("2025-07-10T14:30:00Z");
        trade.setVolume(12345);

        when(svc.getLatestTrade("AAPL")).thenReturn(trade);

        mockMvc.perform(get("/api/prices/AAPL/latest-trade")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(155.75))
                .andExpect(jsonPath("$.timestamp").value("2025-07-10T14:30:00Z"))
                .andExpect(jsonPath("$.volume").value(12345));
    }

    @Test
    void latestTrade_Null_Returns503() throws Exception {
        when(svc.getLatestTrade("AAPL")).thenReturn(null);

        mockMvc.perform(get("/api/prices/AAPL/latest-trade")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());
    }

    // ── getByPeriod ─────────────────────────────────────────────────────

    @Test
    void getByPeriod_Valid_Returns200() throws Exception {
        Instant now = Instant.now();
        List<PriceDataDto> data = List.of(
                new PriceDataDto("AAPL", now, "2025-07-10", new BigDecimal("155.00")),
                new PriceDataDto("AAPL", now.minusSeconds(3600), "2025-07-10", new BigDecimal("154.50"))
        );

        when(svc.findByPeriod(eq("AAPL"), eq(TimePeriod.ONE_DAY))).thenReturn(data);

        mockMvc.perform(get("/api/prices/AAPL/period/1D")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].closingPrice").value(155.00));
    }

    @Test
    void getByPeriod_Invalid_Returns400() throws Exception {
        mockMvc.perform(get("/api/prices/AAPL/period/INVALID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── searchStocks ────────────────────────────────────────────────────────

    @Test
    void searchStocks_Returns200WithResults() throws Exception {
        StockSearchResultDto result = new StockSearchResultDto(List.of(
                new StockSuggestionDto("AAPL", "Apple Inc.", "NASDAQ"),
                new StockSuggestionDto("AMZN", "Amazon.com Inc.", "NASDAQ")
        ));

        when(svc.searchStocks(eq("AA"), eq(10))).thenReturn(result);

        mockMvc.perform(get("/api/prices/search/AA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions.length()").value(2))
                .andExpect(jsonPath("$.suggestions[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.suggestions[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$.suggestions[0].exchange").value("NASDAQ"));
    }

    @Test
    void searchStocks_WithLimit_Returns200() throws Exception {
        StockSearchResultDto result = new StockSearchResultDto(List.of(
                new StockSuggestionDto("AAPL", "Apple Inc.", "NASDAQ")
        ));

        when(svc.searchStocks(eq("A"), eq(5))).thenReturn(result);

        mockMvc.perform(get("/api/prices/search/A?limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions.length()").value(1));
    }

    @Test
    void searchStocks_EmptyResults_Returns200WithEmptyList() throws Exception {
        StockSearchResultDto result = new StockSearchResultDto(List.of());

        when(svc.searchStocks(eq("XYZ123"), eq(10))).thenReturn(result);

        mockMvc.perform(get("/api/prices/search/XYZ123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").isEmpty());
    }

    // ── validateSymbol ──────────────────────────────────────────────────────

    @Test
    void validateSymbol_Valid_Returns200() throws Exception {
        StockValidationDto validation = StockValidationDto.valid("AAPL", "Apple Inc.", "NASDAQ");

        when(svc.validateSymbol("AAPL")).thenReturn(validation);

        mockMvc.perform(get("/api/prices/validate/AAPL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.exchange").value("NASDAQ"))
                .andExpect(jsonPath("$.tradable").value(true));
    }

    @Test
    void validateSymbol_Invalid_Returns404() throws Exception {
        StockValidationDto validation = StockValidationDto.invalid("Stock symbol 'FAKE' not found");

        when(svc.validateSymbol("FAKE")).thenReturn(validation);

        mockMvc.perform(get("/api/prices/validate/FAKE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Stock symbol 'FAKE' not found"));
    }
}
