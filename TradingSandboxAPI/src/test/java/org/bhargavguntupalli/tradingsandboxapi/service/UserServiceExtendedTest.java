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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceExtendedTest {

    @Mock UserRepository userRepo;
    @Mock RoleRepository roleRepo;
    @Mock PasswordEncoder encoder;
    @Mock EmailVerificationTokenRepository tokenRepo;
    @Mock EmailService emailService;

    @InjectMocks
    UserServiceImpl svc;

    private User createTestUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("encoded");
        u.setEmail(username + "@test.com");
        u.setFirstName("Test");
        u.setLastName("User");
        u.setThemePreference(Theme.LIGHT);
        u.setCashBalance(BigDecimal.valueOf(100000.00));
        u.setEmailVerified(true);
        RoleEntity role = new RoleEntity(Role.ROLE_USER);
        u.setRole(role);
        return u;
    }

    // ── verifyEmail ──────────────────────────────────────────────────────

    @Test
    void verifyEmail_ValidToken_VerifiesUser() {
        User user = createTestUser("alice");
        user.setEmailVerified(false);

        EmailVerificationToken vt = new EmailVerificationToken();
        vt.setToken("valid-token");
        vt.setUser(user);
        vt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        vt.setUsed(false);

        when(tokenRepo.findByToken("valid-token")).thenReturn(Optional.of(vt));

        svc.verifyEmail("valid-token");

        assertThat(vt.isUsed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        verify(tokenRepo).save(vt);
        verify(userRepo).save(user);
    }

    @Test
    void verifyEmail_InvalidToken_Throws() {
        when(tokenRepo.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.verifyEmail("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid verification token");
    }

    @Test
    void verifyEmail_UsedToken_Throws() {
        EmailVerificationToken vt = new EmailVerificationToken();
        vt.setUsed(true);
        when(tokenRepo.findByToken("used-token")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> svc.verifyEmail("used-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This verification link has already been used");
    }

    @Test
    void verifyEmail_ExpiredToken_Throws() {
        EmailVerificationToken vt = new EmailVerificationToken();
        vt.setUsed(false);
        vt.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(tokenRepo.findByToken("expired-token")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> svc.verifyEmail("expired-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification link has expired. Please request a new one.");
    }

    // ── resendVerification ───────────────────────────────────────────────

    @Test
    void resendVerification_UnverifiedUser_SendsNewEmail() {
        User user = createTestUser("alice");
        user.setEmailVerified(false);
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(tokenRepo.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.resendVerification("alice@test.com");

        verify(emailService).sendVerificationEmail(eq(user), anyString());
        verify(tokenRepo).save(any(EmailVerificationToken.class));
    }

    @Test
    void resendVerification_AlreadyVerified_Throws() {
        User user = createTestUser("alice");
        user.setEmailVerified(true);
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> svc.resendVerification("alice@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already verified");
    }

    @Test
    void resendVerification_NoAccount_Throws() {
        when(userRepo.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.resendVerification("nobody@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No account found with that email");
    }

    // ── getUserDtoByUsername ──────────────────────────────────────────────

    @Test
    void getUserDtoByUsername_ExistingUser_ReturnsDto() {
        User user = createTestUser("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDto result = svc.getUserDtoByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.getThemePreference()).isEqualTo("LIGHT");
    }

    @Test
    void getUserDtoByUsername_NonExistingUser_Throws() {
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.getUserDtoByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── updateProfile ────────────────────────────────────────────────────

    @Test
    void updateProfile_UpdatesEmailAndNames() {
        User user = createTestUser("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = new UserDto();
        dto.setUsername("alice");
        dto.setEmail("newemail@test.com");
        dto.setFirstName("NewFirst");
        dto.setLastName("NewLast");

        UserDto result = svc.updateProfile("alice", dto);

        assertThat(result.getEmail()).isEqualTo("newemail@test.com");
        assertThat(result.getFirstName()).isEqualTo("NewFirst");
        assertThat(result.getLastName()).isEqualTo("NewLast");
    }

    @Test
    void updateProfile_ChangeUsername_Success() {
        User user = createTestUser("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.findByUsername("newalice")).thenReturn(Optional.empty());
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = new UserDto();
        dto.setUsername("newalice");
        dto.setEmail("alice@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");

        UserDto result = svc.updateProfile("alice", dto);

        assertThat(result.getUsername()).isEqualTo("newalice");
    }

    @Test
    void updateProfile_ChangeToExistingUsername_Throws() {
        User user = createTestUser("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(new User()));

        UserDto dto = new UserDto();
        dto.setUsername("bob");
        dto.setEmail("alice@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");

        assertThatThrownBy(() -> svc.updateProfile("alice", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already in use");
    }

    @Test
    void updateProfile_NullUsername_DoesNotChangeUsername() {
        User user = createTestUser("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = new UserDto();
        dto.setUsername(null);
        dto.setEmail("new@test.com");
        dto.setFirstName("New");
        dto.setLastName("Name");

        UserDto result = svc.updateProfile("alice", dto);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void updateProfile_NonExistingUser_Throws() {
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UserDto dto = new UserDto();
        assertThatThrownBy(() -> svc.updateProfile("nonexistent", dto))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── creditBalance ────────────────────────────────────────────────────

    @Test
    void creditBalance_AddsToBalance() {
        User user = createTestUser("alice");
        user.setCashBalance(BigDecimal.valueOf(1000));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = svc.creditBalance("alice", BigDecimal.valueOf(500));

        assertThat(result.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void creditBalance_NonExistingUser_Throws() {
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.creditBalance("nonexistent", BigDecimal.TEN))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── debitBalance ─────────────────────────────────────────────────────

    @Test
    void debitBalance_SufficientFunds_Deducts() {
        User user = createTestUser("alice");
        user.setCashBalance(BigDecimal.valueOf(1000));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = svc.debitBalance("alice", BigDecimal.valueOf(250));

        assertThat(result.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(750));
    }

    @Test
    void debitBalance_InsufficientFunds_Throws() {
        User user = createTestUser("alice");
        user.setCashBalance(BigDecimal.valueOf(100));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> svc.debitBalance("alice", BigDecimal.valueOf(200)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");
    }

    @Test
    void debitBalance_ExactBalance_Succeeds() {
        User user = createTestUser("alice");
        user.setCashBalance(BigDecimal.valueOf(100));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = svc.debitBalance("alice", BigDecimal.valueOf(100));

        assertThat(result.getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void debitBalance_NonExistingUser_Throws() {
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.debitBalance("nonexistent", BigDecimal.TEN))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── toggleTheme ──────────────────────────────────────────────────────

    @Test
    void toggleTheme_LightToDark() {
        User user = createTestUser("alice");
        user.setThemePreference(Theme.LIGHT);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = svc.toggleTheme("alice");

        assertThat(result.getThemePreference()).isEqualTo("DARK");
    }

    @Test
    void toggleTheme_DarkToLight() {
        User user = createTestUser("alice");
        user.setThemePreference(Theme.DARK);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = svc.toggleTheme("alice");

        assertThat(result.getThemePreference()).isEqualTo("LIGHT");
    }

    @Test
    void toggleTheme_NonExistingUser_Throws() {
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.toggleTheme("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
