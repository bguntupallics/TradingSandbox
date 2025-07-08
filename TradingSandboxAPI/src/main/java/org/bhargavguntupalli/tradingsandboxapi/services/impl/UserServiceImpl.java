package org.bhargavguntupalli.tradingsandboxapi.services.impl;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.models.Role;
import org.bhargavguntupalli.tradingsandboxapi.models.RoleEntity;
import org.bhargavguntupalli.tradingsandboxapi.models.Theme;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.repositories.RoleRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository userRepo,
                           RoleRepository roleRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder  = encoder;
    }

    /** REGISTER a new user, returns a DTO */
    @Override
    @Transactional
    public UserDto registerNewUser(UserDto dto) {
        if (userRepo.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already in use");
        }
        if (userRepo.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        RoleEntity defaultRole = roleRepo.findByName(Role.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setEmail(dto.getEmail());
        u.setFirstName(dto.getFirstName());
        u.setLastName(dto.getLastName());
        u.setThemePreference(Theme.valueOf(dto.getThemePreference()));
        u.setCashBalance(BigDecimal.valueOf(100_000.00));
        u.setRole(defaultRole);

        User saved = userRepo.save(u);
        return toDto(saved);
    }

    /** FETCH current user’s profile */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserDtoByUsername(String username) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        return toDto(u);
    }

    /** UPDATE email, names, theme */
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

    /** CREDIT cash balance */
    @Override
    @Transactional
    public UserDto creditBalance(String username, BigDecimal amount) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        u.setCashBalance(u.getCashBalance().add(amount));
        User saved = userRepo.save(u);
        return toDto(saved);
    }

    /** DEBIT cash balance (throws if insufficient) */
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

    /** Helper to map Entity → DTO */
    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setThemePreference(u.getThemePreference().name());
        dto.setCashBalance(u.getCashBalance());
        return dto;
    }

    @Override
    @Transactional
    public UserDto toggleTheme(String username) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));

        // flip the enum
        Theme next = u.getThemePreference() == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        u.setThemePreference(next);

        User saved = userRepo.save(u);
        return toDto(saved);
    }
}
