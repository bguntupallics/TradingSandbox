package org.bhargavguntupalli.tradingsandboxapi.model;

import org.bhargavguntupalli.tradingsandboxapi.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UserModelTest {

    @Test
    void defaultCashBalance_Is100000() {
        User user = new User();
        assertThat(user.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(100000.00));
    }

    @Test
    void defaultThemePreference_IsLight() {
        User user = new User();
        assertThat(user.getThemePreference()).isEqualTo(Theme.LIGHT);
    }

    @Test
    void settersAndGetters_Work() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hash");
        user.setEmail("alice@test.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setThemePreference(Theme.DARK);
        user.setCashBalance(BigDecimal.valueOf(5000));

        RoleEntity role = new RoleEntity(Role.ROLE_ADMIN);
        user.setRole(role);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("hash");
        assertThat(user.getEmail()).isEqualTo("alice@test.com");
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getThemePreference()).isEqualTo(Theme.DARK);
        assertThat(user.getCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(user.getRole().getName()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void roleEntity_SettersWork() {
        RoleEntity role = new RoleEntity();
        role.setName(Role.ROLE_USER);
        assertThat(role.getName()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void roleEntity_ConstructorSetsName() {
        RoleEntity role = new RoleEntity(Role.ROLE_ADMIN);
        assertThat(role.getName()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void roleEnum_HasTwoValues() {
        assertThat(Role.values()).hasSize(2);
        assertThat(Role.values()).containsExactly(Role.ROLE_USER, Role.ROLE_ADMIN);
    }

    @Test
    void themeEnum_HasTwoValues() {
        assertThat(Theme.values()).hasSize(2);
        assertThat(Theme.values()).containsExactly(Theme.LIGHT, Theme.DARK);
    }
}
