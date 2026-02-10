package org.bhargavguntupalli.tradingsandboxapi.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {
        cookieUtil = new CookieUtil();
        ReflectionTestUtils.setField(cookieUtil, "expirationMs", 3600000L);
        ReflectionTestUtils.setField(cookieUtil, "secureCookie", false);
        ReflectionTestUtils.setField(cookieUtil, "cookieDomain", "");
    }

    @Test
    void addJwtCookie_SetsCookieOnResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addJwtCookie(response, "test-jwt-token");

        Cookie cookie = response.getCookie("jwt");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("test-jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(3600);
    }

    @Test
    void addJwtCookie_SecureMode_SetSecureFlag() {
        ReflectionTestUtils.setField(cookieUtil, "secureCookie", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addJwtCookie(response, "token");

        Cookie cookie = response.getCookie("jwt");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    void addJwtCookie_WithDomain_SetsDomain() {
        ReflectionTestUtils.setField(cookieUtil, "cookieDomain", "example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addJwtCookie(response, "token");

        Cookie cookie = response.getCookie("jwt");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getDomain()).isEqualTo("example.com");
    }

    @Test
    void clearJwtCookie_SetsMaxAgeToZero() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.clearJwtCookie(response);

        Cookie cookie = response.getCookie("jwt");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void jwtCookieName_IsJwt() {
        assertThat(CookieUtil.JWT_COOKIE_NAME).isEqualTo("jwt");
    }
}
