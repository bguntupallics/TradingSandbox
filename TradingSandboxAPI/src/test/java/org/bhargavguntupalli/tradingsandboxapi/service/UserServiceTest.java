package org.bhargavguntupalli.tradingsandboxapi.service;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.models.*;
import org.bhargavguntupalli.tradingsandboxapi.repositories.EmailVerificationTokenRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.RoleRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.EmailService;
import org.bhargavguntupalli.tradingsandboxapi.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepo;
    @Mock RoleRepository roleRepo;
    @Mock PasswordEncoder encoder;
    @Mock EmailVerificationTokenRepository tokenRepo;
    @Mock EmailService emailService;

    @InjectMocks
    UserServiceImpl svc;

    @Captor ArgumentCaptor<User> userCaptor;
    @Captor ArgumentCaptor<EmailVerificationToken> tokenCaptor;

    @Test
    void registerNewUser_Success_AutoGeneratesUsernameFromEmail() {
        // given
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setName(Role.ROLE_USER);
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.of(role));
        when(encoder.encode("passwd")).thenReturn("ENC(passwd)");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.<User>getArgument(0));
        when(tokenRepo.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserDto result = svc.registerNewUser(dto);

        // then
        verify(userRepo).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPassword()).isEqualTo("ENC(passwd)");
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getThemePreference()).isEqualTo(Theme.LIGHT);
        assertThat(saved.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.isEmailVerified()).isFalse();
    }

    @Test
    void registerNewUser_SendsVerificationEmail() {
        // given
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity(Role.ROLE_USER);
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.of(role));
        when(encoder.encode("passwd")).thenReturn("ENC");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepo.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        svc.registerNewUser(dto);

        // then
        verify(emailService).sendVerificationEmail(any(User.class), anyString());
        verify(tokenRepo).save(any(EmailVerificationToken.class));
    }

    @Test
    void registerNewUser_DuplicateVerifiedEmail_Throws() {
        // given
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");

        User existingVerified = new User();
        existingVerified.setEmailVerified(true);
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(existingVerified));

        // when / then
        assertThatThrownBy(() -> svc.registerNewUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void registerNewUser_DuplicateUnverifiedEmail_DeletesAndReRegisters() {
        // given
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");

        User existingUnverified = new User();
        existingUnverified.setEmailVerified(false);
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUnverified));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity(Role.ROLE_USER);
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.of(role));
        when(encoder.encode("passwd")).thenReturn("ENC");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepo.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserDto result = svc.registerNewUser(dto);

        // then
        verify(userRepo).delete(existingUnverified);
        verify(userRepo).flush();
        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void registerNewUser_UsernameConflict_AppendsSuffix() {
        // given
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        // "alice" already exists, "alice1" is free
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(new User()));
        when(userRepo.findByUsername("alice1")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity(Role.ROLE_USER);
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.of(role));
        when(encoder.encode("passwd")).thenReturn("ENC");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepo.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        UserDto result = svc.registerNewUser(dto);

        // then
        assertThat(result.getUsername()).isEqualTo("alice1");
    }

    @Test
    void registerNewUser_MissingRole_Throws() {
        UserDto dto = new UserDto();
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");

        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.registerNewUser(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ROLE_USER not found");
    }
}
