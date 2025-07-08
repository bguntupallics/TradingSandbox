package org.bhargavguntupalli.tradingsandboxapi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * A simple ping/stats endpoint for admins.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        // Return whatever you like here—this is just a placeholder.
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now(),
                "message", "Admin access granted"
        );
    }
}
