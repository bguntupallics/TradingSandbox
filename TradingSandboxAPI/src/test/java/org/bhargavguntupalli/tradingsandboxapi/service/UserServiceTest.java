package org.bhargavguntupalli.tradingsandboxapi.service;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.models.Role;
import org.bhargavguntupalli.tradingsandboxapi.models.RoleEntity;
import org.bhargavguntupalli.tradingsandboxapi.models.Theme;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.repositories.RoleRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.bhargavguntupalli.tradingsandboxapi.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepo;
    @Mock RoleRepository roleRepo;
    @Mock PasswordEncoder encoder;

    @InjectMocks
    UserServiceImpl svc;

    @Captor ArgumentCaptor<User> userCaptor;

    @Test
    void registerNewUser_Success() {
        // given
        UserDto dto = new UserDto();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("passwd");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setThemePreference("LIGHT");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setName(Role.ROLE_USER);
        when(roleRepo.findByName(Role.ROLE_USER)).thenReturn(Optional.of(role));
        when(encoder.encode("passwd")).thenReturn("ENC(passwd)");

        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            return inv.<User>getArgument(0);
        });

        // when
        UserDto result = svc.registerNewUser(dto);

        // then
        verify(userRepo).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        System.out.println(result.toString());

        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPassword()).isEqualTo("ENC(passwd)");
        assertThat(saved.getThemePreference()).isEqualTo(Theme.LIGHT);
        assertThat(saved.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void registerNewUser_DuplicateUsername_Throws() {
        // given
        UserDto dto = new UserDto();
        dto.setUsername("bob");
        dto.setEmail("bob@example.com");
        when(userRepo.findByUsername("bob"))
                .thenReturn(Optional.of(new User()));

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> svc.registerNewUser(dto)
        );
        assertThat(ex).hasMessage("Username already in use");
    }
}
