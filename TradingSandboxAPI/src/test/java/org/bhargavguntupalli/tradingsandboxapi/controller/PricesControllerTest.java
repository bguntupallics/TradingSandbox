package org.bhargavguntupalli.tradingsandboxapi.controller;

import org.bhargavguntupalli.tradingsandboxapi.dto.MarketStatusDto;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest   // load only your controllers + related MVC config
@AutoConfigureMockMvc(addFilters = false)   // ← disables the JWT/security filter
class PricesControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DailyPriceService svc;  // mock the service your controller depends on

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

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
                .andExpect(jsonPath("$.nextOpen").value(nextOpen.toString()))
                .andExpect(jsonPath("$.nextClose").value(nextClose.toString()));
    }
}
