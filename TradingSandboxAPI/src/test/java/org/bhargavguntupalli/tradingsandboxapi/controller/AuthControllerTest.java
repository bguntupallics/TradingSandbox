package org.bhargavguntupalli.tradingsandboxapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bhargavguntupalli.tradingsandboxapi.controllers.AuthController;
import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    JwtProvider jwtProvider;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    DailyPriceService svc;

    // ── Register ────────────────────────────────────────────────────────

    @Test
    void register_Success_ReturnsUserDto() throws Exception {
        String payload = """
                {
                  "username": "johndoe",
                  "password": "Secret123!",
                  "email": "john@example.com",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        UserDto result = new UserDto();
        result.setId(1L);
        result.setUsername("johndoe");
        result.setEmail("john@example.com");
        result.setFirstName("John");
        result.setLastName("Doe");

        when(userService.registerNewUser(any(UserDto.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void register_DuplicateUsername_Returns400() throws Exception {
        String payload = """
                {
                  "username": "johndoe",
                  "password": "Secret123!",
                  "email": "john@example.com",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        when(userService.registerNewUser(any(UserDto.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // ── Login ───────────────────────────────────────────────────────────

    @Test
    void login_Success_ReturnsToken() throws Exception {
        String payload = """
                {
                  "username": "johndoe",
                  "password": "Secret123!"
                }
                """;

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtProvider.generateToken(mockAuth)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void login_BadCredentials_Returns401() throws Exception {
        String payload = """
                {
                  "username": "johndoe",
                  "password": "WrongPassword"
                }
                """;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
