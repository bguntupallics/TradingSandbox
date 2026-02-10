package org.bhargavguntupalli.tradingsandboxapi.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTests {

    // ── DailyPriceDto ────────────────────────────────────────────────────

    @Test
    void dailyPriceDto_Constructor_SetsAllFields() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceDto dto = new DailyPriceDto("AAPL", date, new BigDecimal("150.25"));

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getDate()).isEqualTo(date);
        assertThat(dto.getClosingPrice()).isEqualByComparingTo(new BigDecimal("150.25"));
    }

    @Test
    void dailyPriceDto_Setters_UpdateFields() {
        DailyPriceDto dto = new DailyPriceDto("AAPL", LocalDate.now(), BigDecimal.TEN);
        dto.setSymbol("GOOG");
        dto.setDate(LocalDate.of(2025, 1, 1));
        dto.setClosingPrice(BigDecimal.valueOf(200));

        assertThat(dto.getSymbol()).isEqualTo("GOOG");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(dto.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    // ── LoginRequest ─────────────────────────────────────────────────────

    @Test
    void loginRequest_GettersAndSetters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("testuser@example.com");
        req.setPassword("secret");

        assertThat(req.getEmail()).isEqualTo("testuser@example.com");
        assertThat(req.getPassword()).isEqualTo("secret");
    }

    // ── UserDto ──────────────────────────────────────────────────────────

    @Test
    void userDto_GettersAndSetters() {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("johndoe");
        dto.setPassword("secret");
        dto.setEmail("john@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setThemePreference("DARK");
        dto.setAmount(BigDecimal.valueOf(500));
        dto.setCashBalance(BigDecimal.valueOf(10000));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("johndoe");
        assertThat(dto.getPassword()).isEqualTo("secret");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getThemePreference()).isEqualTo("DARK");
        assertThat(dto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(dto.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    // ── TradeResponseDto ─────────────────────────────────────────────────

    @Test
    void tradeResponseDto_GettersAndSetters() {
        TradeResponseDto dto = new TradeResponseDto();
        dto.setPrice(155.75);
        dto.setTimestamp("2025-07-10T14:30:00Z");
        dto.setVolume(12345);

        assertThat(dto.getPrice()).isEqualTo(155.75);
        assertThat(dto.getTimestamp()).isEqualTo("2025-07-10T14:30:00Z");
        assertThat(dto.getVolume()).isEqualTo(12345);
    }

    // ── MarketStatusDto ──────────────────────────────────────────────────

    @Test
    void marketStatusDto_DefaultConstructor() {
        MarketStatusDto dto = new MarketStatusDto();
        assertThat(dto.isOpen()).isFalse();
        assertThat(dto.getNextOpen()).isNull();
        assertThat(dto.getNextClose()).isNull();
    }

    @Test
    void marketStatusDto_ParameterizedConstructor() {
        OffsetDateTime nextOpen = OffsetDateTime.parse("2025-07-10T09:30:00-04:00");
        OffsetDateTime nextClose = OffsetDateTime.parse("2025-07-10T16:00:00-04:00");
        MarketStatusDto dto = new MarketStatusDto(true, nextOpen, nextClose);

        assertThat(dto.isOpen()).isTrue();
        assertThat(dto.getNextOpen()).isEqualTo(nextOpen);
        assertThat(dto.getNextClose()).isEqualTo(nextClose);
    }

    @Test
    void marketStatusDto_SettersOverrideValues() {
        MarketStatusDto dto = new MarketStatusDto();
        OffsetDateTime nextOpen = OffsetDateTime.parse("2025-07-10T09:30:00-04:00");
        dto.setOpen(true);
        dto.setNextOpen(nextOpen);

        assertThat(dto.isOpen()).isTrue();
        assertThat(dto.getNextOpen()).isEqualTo(nextOpen);
    }

    // ── PriceDataDto ─────────────────────────────────────────────────────

    @Test
    void priceDataDto_DefaultConstructor() {
        PriceDataDto dto = new PriceDataDto();
        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getTimestamp()).isNull();
        assertThat(dto.getDateLabel()).isNull();
        assertThat(dto.getClosingPrice()).isNull();
    }

    @Test
    void priceDataDto_ParameterizedConstructor() {
        Instant now = Instant.now();
        PriceDataDto dto = new PriceDataDto("AAPL", now, "7/10", BigDecimal.valueOf(155));

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getTimestamp()).isEqualTo(now);
        assertThat(dto.getDateLabel()).isEqualTo("7/10");
        assertThat(dto.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(155));
    }

    @Test
    void priceDataDto_SettersWork() {
        PriceDataDto dto = new PriceDataDto();
        Instant now = Instant.now();
        dto.setSymbol("GOOG");
        dto.setTimestamp(now);
        dto.setDateLabel("1/1");
        dto.setClosingPrice(BigDecimal.valueOf(2800));

        assertThat(dto.getSymbol()).isEqualTo("GOOG");
        assertThat(dto.getTimestamp()).isEqualTo(now);
        assertThat(dto.getDateLabel()).isEqualTo("1/1");
        assertThat(dto.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(2800));
    }

    // ── BarDto ───────────────────────────────────────────────────────────

    @Test
    void barDto_GettersAndSetters() {
        BarDto dto = new BarDto();
        Instant now = Instant.now();

        dto.setSymbol("NVDA");
        dto.setTimestamp(now);
        dto.setOpen(100.0);
        dto.setHigh(110.0);
        dto.setLow(95.0);
        dto.setClose(105.0);
        dto.setVolume(1000000.0);
        dto.setTradeCount(50000.0);
        dto.setVwap(102.5);

        assertThat(dto.getSymbol()).isEqualTo("NVDA");
        assertThat(dto.getTimestamp()).isEqualTo(now);
        assertThat(dto.getOpen()).isEqualTo(100.0);
        assertThat(dto.getHigh()).isEqualTo(110.0);
        assertThat(dto.getLow()).isEqualTo(95.0);
        assertThat(dto.getClose()).isEqualTo(105.0);
        assertThat(dto.getVolume()).isEqualTo(1000000.0);
        assertThat(dto.getTradeCount()).isEqualTo(50000.0);
        assertThat(dto.getVwap()).isEqualTo(102.5);
    }

    // ── BarDataDto ───────────────────────────────────────────────────────

    @Test
    void barDataDto_DefaultConstructor() {
        BarDataDto dto = new BarDataDto();
        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getBars()).isNull();
    }

    @Test
    void barDataDto_ParameterizedConstructor() {
        BarDto bar = new BarDto();
        bar.setClose(150.0);
        Map<String, List<BarDto>> bars = Map.of("AAPL", List.of(bar));
        LocalDateTime start = LocalDateTime.of(2025, 7, 9, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 7, 10, 0, 0);

        BarDataDto dto = new BarDataDto("AAPL", start, end, "1Day", bars);

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getStartDate()).isEqualTo(start);
        assertThat(dto.getEndDate()).isEqualTo(end);
        assertThat(dto.getTimeframe()).isEqualTo("1Day");
        assertThat(dto.getBars()).containsKey("AAPL");
        assertThat(dto.getBars().get("AAPL")).hasSize(1);
    }

    @Test
    void barDataDto_SettersWork() {
        BarDataDto dto = new BarDataDto();
        dto.setSymbol("GOOG");
        dto.setTimeframe("1Hour");
        dto.setStartDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        dto.setEndDate(LocalDateTime.of(2025, 1, 2, 0, 0));
        dto.setBars(Map.of());

        assertThat(dto.getSymbol()).isEqualTo("GOOG");
        assertThat(dto.getTimeframe()).isEqualTo("1Hour");
        assertThat(dto.getBars()).isEmpty();
    }
}
