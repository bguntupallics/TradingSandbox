package org.bhargavguntupalli.tradingsandboxapi.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Key;

@Configuration
public class JwtKeyConfig {

    @Bean
    public Key jwtSigningKey() {
        // Generates a random 256-bit key for HS256 on each startup
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }
}
