package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Override
    public void sendVerificationEmail(User user, String token) {
        String verificationLink = backendUrl + "/api/auth/verify?token=" + token;
        log.info("""

                ========================================
                EMAIL VERIFICATION (mock)
                To:      {}
                Subject: Verify your TradingSandbox account

                Click the link below to verify your email:
                {}

                This link expires in 24 hours.
                ========================================
                """, user.getEmail(), verificationLink);
    }
}
