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

    private User createTestUser(String username, String password, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setEmail(username + "@test.com");
        u.setFirstName("Test");
        u.setLastName("User");
        RoleEntity roleEntity = new RoleEntity(role);
        u.setRole(roleEntity);
        return u;
    }

    @Test
    void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
        User user = createTestUser("alice", "encoded-pass", Role.ROLE_USER);
        when(repo.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded-pass");
    }

    @Test
    void loadUserByUsername_ExistingUser_HasCorrectAuthority() {
        User user = createTestUser("alice", "encoded-pass", Role.ROLE_USER);
        when(repo.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_AdminUser_HasAdminAuthority() {
        User user = createTestUser("admin", "encoded-pass", Role.ROLE_ADMIN);
        when(repo.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_NonExistingUser_ThrowsUsernameNotFoundException() {
        when(repo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("nonexistent");
    }
}
