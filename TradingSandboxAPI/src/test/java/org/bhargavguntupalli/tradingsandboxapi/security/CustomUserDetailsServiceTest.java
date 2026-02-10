package org.bhargavguntupalli.tradingsandboxapi.security;

import org.bhargavguntupalli.tradingsandboxapi.models.Role;
import org.bhargavguntupalli.tradingsandboxapi.models.RoleEntity;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository repo;

    @InjectMocks
    CustomUserDetailsService service;

    private User createTestUser(String username, String email, String password, Role role, boolean emailVerified) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setEmail(email);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmailVerified(emailVerified);
        RoleEntity roleEntity = new RoleEntity(role);
        u.setRole(roleEntity);
        return u;
    }

    @Test
    void loadUserByUsername_ExistingVerifiedUser_ReturnsEnabledUserDetails() {
        User user = createTestUser("alice", "alice@test.com", "encoded-pass", Role.ROLE_USER, true);
        when(repo.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice@test.com");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded-pass");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_UnverifiedUser_ReturnsDisabledUserDetails() {
        User user = createTestUser("bob", "bob@test.com", "encoded-pass", Role.ROLE_USER, false);
        when(repo.findByEmail("bob@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bob@test.com");

        assertThat(details.getUsername()).isEqualTo("bob");
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_ExistingUser_HasCorrectAuthority() {
        User user = createTestUser("alice", "alice@test.com", "encoded-pass", Role.ROLE_USER, true);
        when(repo.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice@test.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_AdminUser_HasAdminAuthority() {
        User user = createTestUser("admin", "admin@test.com", "encoded-pass", Role.ROLE_ADMIN, true);
        when(repo.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_NonExistingEmail_ThrowsUsernameNotFoundException() {
        when(repo.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nonexistent@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent@test.com");
    }
}
