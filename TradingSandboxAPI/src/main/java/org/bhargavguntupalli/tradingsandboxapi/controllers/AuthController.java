package org.bhargavguntupalli.tradingsandboxapi.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.bhargavguntupalli.tradingsandboxapi.dto.LoginRequest;
import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.security.CookieUtil;
import org.bhargavguntupalli.tradingsandboxapi.security.JwtProvider;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public AuthController(UserService userService,
                          AuthenticationManager authManager,
                          JwtProvider jwtProvider,
                          CookieUtil cookieUtil) {
        this.userService = userService;
        this.authManager = authManager;
        this.jwtProvider = jwtProvider;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody UserDto dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(f -> f.getField() + ": " + f.getDefaultMessage())
                    .toList();
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("errors", errors));
        }

        try {
            userService.registerNewUser(dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful. Please check your email to verify your account."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(path = {"/login", "/login/"})
    public ResponseEntity<?> login(@RequestBody LoginRequest dto, HttpServletResponse response) {
        try {
            // AuthenticationManager calls CustomUserDetailsService.loadUserByUsername(email)
            // which throws DisabledException if email is not verified
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            String token = jwtProvider.generateToken(auth);
            cookieUtil.addJwtCookie(response, token);

            return ResponseEntity.ok(Map.of("message", "Login successful"));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Email not verified",
                    "code", "EMAIL_NOT_VERIFIED"
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        cookieUtil.clearJwtCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getUserDtoByUsername(auth.getName()));
    }

    @GetMapping("/verify")
    public void verifyEmail(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            userService.verifyEmail(token);
            response.sendRedirect(frontendUrl + "/login?verified=true");
        } catch (IllegalArgumentException e) {
            response.sendRedirect(frontendUrl + "/login?error=" + e.getMessage());
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        try {
            userService.resendVerification(email);
            return ResponseEntity.ok(Map.of("message", "Verification email sent"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
