package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Role name;

    public RoleEntity() {}

    public RoleEntity(Role name) {
        this.name = name;
    }
}
