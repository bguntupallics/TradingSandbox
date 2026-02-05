package org.bhargavguntupalli.tradingsandboxapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bhargavguntupalli.tradingsandboxapi.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtLoginFilterTest {

    @Mock
    AuthenticationManager authManager;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    FilterChain filterChain;

    JwtLoginFilter loginFilter;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        loginFilter = new JwtLoginFilter(authManager, jwtProvider);
    }

    @Test
    void attemptAuthentication_ValidCredentials_DelegatesToAuthManager() throws Exception {
        LoginRequest creds = new LoginRequest();
        creds.setUsername("testuser");
        creds.setPassword("password123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(objectMapper.writeValueAsBytes(creds));
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication expected = new UsernamePasswordAuthenticationToken(
                "testuser", "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(expected);

        Authentication result = loginFilter.attemptAuthentication(request, response);

        assertThat(result.getName()).isEqualTo("testuser");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void attemptAuthentication_BadCredentials_ThrowsException() throws Exception {
        LoginRequest creds = new LoginRequest();
        creds.setUsername("testuser");
        creds.setPassword("wrongpass");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(objectMapper.writeValueAsBytes(creds));
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginFilter.attemptAuthentication(request, response))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void successfulAuthentication_WritesTokenToResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(jwtProvider.generateToken(auth)).thenReturn("jwt-token-123");

        loginFilter.successfulAuthentication(request, response, filterChain, auth);

        assertThat(response.getContentType()).isEqualTo("application/json");
        @SuppressWarnings("unchecked")
        Map<String, String> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("token", "jwt-token-123");
    }

    @Test
    void unsuccessfulAuthentication_Returns401WithErrorMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        loginFilter.unsuccessfulAuthentication(request, response, new BadCredentialsException("Bad creds"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        @SuppressWarnings("unchecked")
        Map<String, String> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(body).containsEntry("error", "Invalid username or password");
    }
}
