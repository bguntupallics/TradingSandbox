package org.bhargavguntupalli.tradingsandboxapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bhargavguntupalli.tradingsandboxapi.controllers.AuthController;
import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.security.CookieUtil;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    CookieUtil cookieUtil;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    DailyPriceService svc;

    // ── Register ────────────────────────────────────────────────────────

    @Test
    void register_Success_ReturnsMessage() throws Exception {
        String payload = """
                {
                  "email": "john@example.com",
                  "password": "Secret123!",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        UserDto result = new UserDto();
        result.setId(1L);
        result.setUsername("john");
        result.setEmail("john@example.com");
        result.setFirstName("John");
        result.setLastName("Doe");
        result.setEmailVerified(false);

        when(userService.registerNewUser(any(UserDto.class))).thenReturn(result);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));
    }

    @Test
    void register_DuplicateEmail_ReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "email": "john@example.com",
                  "password": "Secret123!",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        when(userService.registerNewUser(any(UserDto.class)))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    // ── Login ───────────────────────────────────────────────────────────

    @Test
    void login_Success_SetsCookieAndReturnsMessage() throws Exception {
        String payload = """
                {
                  "email": "john@example.com",
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
                .andExpect(jsonPath("$.message").value("Login successful"));

        verify(cookieUtil).addJwtCookie(any(), eq("mock-jwt-token"));
    }

    @Test
    void login_BadCredentials_Returns401() throws Exception {
        String payload = """
                {
                  "email": "john@example.com",
                  "password": "WrongPassword"
                }
                """;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void login_EmailNotVerified_Returns403() throws Exception {
        String payload = """
                {
                  "email": "john@example.com",
                  "password": "Secret123!"
                }
                """;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Email not verified"))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    // ── Logout ──────────────────────────────────────────────────────────

    @Test
    void logout_ClearsCookieAndReturnsMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));

        verify(cookieUtil).clearJwtCookie(any());
    }

    // ── Me ───────────────────────────────────────────────────────────────

    @Test
    void me_Authenticated_ReturnsUserDto() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("johndoe");
        dto.setEmail("john@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmailVerified(true);

        when(userService.getUserDtoByUsername("johndoe")).thenReturn(dto);

        mockMvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("johndoe", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void me_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── Verify Email ─────────────────────────────────────────────────────

    @Test
    void verify_ValidToken_RedirectsToLoginWithVerified() throws Exception {
        doNothing().when(userService).verifyEmail("valid-token");

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "valid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/login?verified=true"));
    }

    @Test
    void verify_InvalidToken_RedirectsWithError() throws Exception {
        doThrow(new IllegalArgumentException("Invalid verification token"))
                .when(userService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "bad-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/login?error=Invalid verification token"));
    }

    // ── Resend Verification ──────────────────────────────────────────────

    @Test
    void resendVerification_Success_ReturnsMessage() throws Exception {
        doNothing().when(userService).resendVerification("john@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "john@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent"));
    }

    @Test
    void resendVerification_NoAccount_ReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("No account found with that email"))
                .when(userService).resendVerification("unknown@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "unknown@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No account found with that email"));
    }

    @Test
    void resendVerification_MissingEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required"));
    }

    @Test
    void resendVerification_AlreadyVerified_ReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Email is already verified"))
                .when(userService).resendVerification("verified@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "verified@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already verified"));
    }
}
