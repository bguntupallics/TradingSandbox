package org.bhargavguntupalli.tradingsandboxapi.security;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private Key key;

    @BeforeEach
    void setUp() {
        // 32-byte secret for HS256
        String secret = "this-is-a-very-long-secret-key-for-testing-purposes-1234567890";
        key = Keys.hmacShaKeyFor(secret.getBytes());
        jwtProvider = new JwtProvider(key, 3600000L); // 1 hour
    }

    @Test
    void generateToken_ReturnsNonNullToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = jwtProvider.generateToken(auth);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void getUsernameFromToken_ReturnsCorrectUsername() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "johndoe", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtProvider.generateToken(auth);

        String username = jwtProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("johndoe");
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtProvider.generateToken(auth);

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        assertThat(jwtProvider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Create provider with 0ms expiration
        JwtProvider expiredProvider = new JwtProvider(key, 0L);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = expiredProvider.generateToken(auth);

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }

    @Test
    void getRolesFromToken_ReturnsSingleRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtProvider.generateToken(auth);

        List<String> roles = jwtProvider.getRolesFromToken(token);

        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    void getRolesFromToken_ReturnsMultipleRoles() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );
        String token = jwtProvider.generateToken(auth);

        List<String> roles = jwtProvider.getRolesFromToken(token);

        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void generateToken_DifferentUsers_ProduceDifferentTokens() {
        Authentication auth1 = new UsernamePasswordAuthenticationToken(
                "user1", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Authentication auth2 = new UsernamePasswordAuthenticationToken(
                "user2", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token1 = jwtProvider.generateToken(auth1);
        String token2 = jwtProvider.generateToken(auth2);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void validateToken_TokenFromDifferentKey_ReturnsFalse() {
        // Generate token with current provider
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtProvider.generateToken(auth);

        // Create a different provider with a different key
        String differentSecret = "a-completely-different-secret-key-for-testing-abcdefgh";
        Key differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes());
        JwtProvider differentProvider = new JwtProvider(differentKey, 3600000L);

        assertThat(differentProvider.validateToken(token)).isFalse();
    }
}
