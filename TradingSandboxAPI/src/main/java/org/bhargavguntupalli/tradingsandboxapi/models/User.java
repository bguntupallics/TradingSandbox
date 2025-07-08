package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {
    @Getter
    @Id @GeneratedValue
    private Long id;

    @Getter @Setter
    @Column(unique = true, nullable = false)
    private String username;

    @Getter @Setter
    @Column(nullable = false)
    private String password;  // store BCrypt hash

    @Getter @Setter
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Getter @Setter
    @Column(unique = true, nullable = false)
    private String email;

    @Getter @Setter
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Getter @Setter
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false)
    private Theme themePreference = Theme.LIGHT;

    @Getter @Setter
    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashBalance = BigDecimal.valueOf(100000.00);
}
