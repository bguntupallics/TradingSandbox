package org.bhargavguntupalli.tradingsandboxapi.services;

import org.bhargavguntupalli.tradingsandboxapi.models.User;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
}
