package org.bhargavguntupalli.tradingsandboxapi.service;

import org.bhargavguntupalli.tradingsandboxapi.dto.BarDataDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.BarDto;
import org.bhargavguntupalli.tradingsandboxapi.dto.DailyPriceDto;
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
}
