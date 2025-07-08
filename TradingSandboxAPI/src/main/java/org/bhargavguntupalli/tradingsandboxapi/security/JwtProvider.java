package org.bhargavguntupalli.tradingsandboxapi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtProvider {
    private final Key key;
    private final long expirationMs;
    private final JwtParser parser;

    public JwtProvider(Key jwtSigningKey, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = jwtSigningKey;
        this.expirationMs = expirationMs;
        this.parser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    public String generateToken(Authentication authentication) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        // 1) collect the roles as strings
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)    // <-- make sure this is GrantedAuthority!
                .collect(Collectors.toList());

        // 2) build the JWT
        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = parser
                .parseClaimsJws(token)
                .getBody();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return roles == null ? List.of() : roles;
    }
}
