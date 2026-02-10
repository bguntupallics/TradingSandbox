package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.models.*;
import org.bhargavguntupalli.tradingsandboxapi.repositories.EmailVerificationTokenRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.RoleRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.EmailService;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final EmailVerificationTokenRepository tokenRepo;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepo,
                           RoleRepository roleRepo,
                           PasswordEncoder encoder,
                           EmailVerificationTokenRepository tokenRepo,
                           EmailService emailService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder  = encoder;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public UserDto registerNewUser(UserDto dto) {
        Optional<User> existingByEmail = userRepo.findByEmail(dto.getEmail());
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (existing.isEmailVerified()) {
                throw new IllegalArgumentException("Email already in use");
            }
            // Unverified account exists — delete it so the user can re-register
            userRepo.delete(existing);
            userRepo.flush();
        }

        RoleEntity defaultRole = roleRepo.findByName(Role.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        // Auto-generate username from email prefix
        String baseUsername = dto.getEmail().substring(0, dto.getEmail().indexOf('@'));
        String username = ensureUniqueUsername(baseUsername);

        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setEmail(dto.getEmail());
        u.setFirstName(dto.getFirstName());
        u.setLastName(dto.getLastName());
        u.setThemePreference(Theme.LIGHT);
        u.setCashBalance(BigDecimal.valueOf(100_000.00));
        u.setRole(defaultRole);
        u.setEmailVerified(false);

        User saved = userRepo.save(u);

        // Generate and send verification token
        String token = createVerificationToken(saved);
        emailService.sendVerificationEmail(saved, token);

        return toDto(saved);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken vt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (vt.isUsed()) {
            throw new IllegalArgumentException("This verification link has already been used");
        }

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification link has expired. Please request a new one.");
        }

        vt.setUsed(true);
        tokenRepo.save(vt);

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        String token = createVerificationToken(user);
        emailService.sendVerificationEmail(user, token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserDtoByUsername(String username) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return toDto(u);
    }

    @Override
    @Transactional
    public UserDto updateProfile(String username, UserDto dto) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        if (dto.getUsername() != null && !dto.getUsername().equals(username)) {
            if (userRepo.findByUsername(dto.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already in use");
            }

            u.setUsername(dto.getUsername());
        }

        u.setEmail(dto.getEmail());
        u.setFirstName(dto.getFirstName());
        u.setLastName(dto.getLastName());

        User saved = userRepo.save(u);
        return toDto(saved);
    }

    @Override
    @Transactional
    public UserDto creditBalance(String username, BigDecimal amount) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        u.setCashBalance(u.getCashBalance().add(amount));
        User saved = userRepo.save(u);
        return toDto(saved);
    }

    @Override
    @Transactional
    public UserDto debitBalance(String username, BigDecimal amount) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        if (u.getCashBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        u.setCashBalance(u.getCashBalance().subtract(amount));
        User saved = userRepo.save(u);
        return toDto(saved);
    }

    @Override
    @Transactional
    public UserDto toggleTheme(String username) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        Theme next = u.getThemePreference() == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        u.setThemePreference(next);

        User saved = userRepo.save(u);
        return toDto(saved);
    }

    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setEmailVerified(u.isEmailVerified());
        dto.setThemePreference(u.getThemePreference().name());
        dto.setCashBalance(u.getCashBalance());
        return dto;
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (userRepo.findByUsername(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String createVerificationToken(User user) {
        String tokenStr = UUID.randomUUID().toString();
        EmailVerificationToken vt = new EmailVerificationToken();
        vt.setToken(tokenStr);
        vt.setUser(user);
        vt.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        vt.setUsed(false);
        tokenRepo.save(vt);
        return tokenStr;
    }
}
