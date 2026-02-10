package org.bhargavguntupalli.tradingsandboxapi.repositories;

import org.bhargavguntupalli.tradingsandboxapi.models.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
}
