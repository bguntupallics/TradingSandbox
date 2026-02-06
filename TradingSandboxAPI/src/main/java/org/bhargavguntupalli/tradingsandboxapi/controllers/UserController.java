package org.bhargavguntupalli.tradingsandboxapi.controllers;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
//import java.math.RoundingMode;

@RestController
@RequestMapping("/api/account")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET current user’s profile */
    @GetMapping(path = {"", "/"})
    public ResponseEntity<UserDto> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getUserDtoByUsername(auth.getName()));
    }

    /** GET user's current balance */
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication auth) {
        BigDecimal balance = userService.getUserDtoByUsername(auth.getName()).getCashBalance();
        return ResponseEntity.ok(balance);
    }

    /** GET user's current theme */
    @GetMapping("/theme")
    public ResponseEntity<String> getTheme(Authentication auth) {
        return ResponseEntity.ok(userService.getUserDtoByUsername(auth.getName()).getThemePreference());
    }

    /** UPDATE email, names, theme */
    @PostMapping("/update")
    public ResponseEntity<UserDto> updateProfile(
            Authentication auth,
            @Validated(UserDto.UpdateProfile.class) @RequestBody UserDto dto
    ) {
        return ResponseEntity.ok(userService.updateProfile(auth.getName(), dto));
    }

    /** CREDIT cash balance */
    @PostMapping("/update/balance/credit")
    public ResponseEntity<UserDto> creditBalance(
            Authentication auth,
            @Validated(UserDto.ChangeBalance.class) @RequestBody UserDto dto
    ) {
        return ResponseEntity.ok(userService.creditBalance(auth.getName(), dto.getAmount()));
    }

    /** DEBIT cash balance */
    @PostMapping("/update/balance/debit")
    public ResponseEntity<UserDto> debitBalance(
            Authentication auth, @Validated(UserDto.ChangeBalance.class) @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.debitBalance(auth.getName(), dto.getAmount()));
    }

    @PostMapping("/change-theme")
    public ResponseEntity<UserDto> changeTheme(Authentication auth) {
        UserDto updated = userService.toggleTheme(auth.getName());
        return ResponseEntity.ok(updated);
    }
}
