package org.bhargavguntupalli.tradingsandboxapi.controllers;

import org.bhargavguntupalli.tradingsandboxapi.dto.LoginRequest;
import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.security.JwtProvider;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtProvider jwtProvider;

    public AuthController(UserService userService, AuthenticationManager authManager, JwtProvider jwtProvider) {
        this.userService = userService;
        this.authManager = authManager;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody UserDto dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            // collect all error messages into a single response
            var errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(f -> f.getField() + ": " + f.getDefaultMessage())
                    .toList();
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("errors", errors));
        }

        UserDto result = userService.registerNewUser(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = {"/login", "/login/"})
    public ResponseEntity<?> login(@RequestBody LoginRequest dto) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        String token = jwtProvider.generateToken(auth);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "type",  "Bearer"
        ));
    }
}
