package org.bhargavguntupalli.tradingsandboxapi.config;

import org.bhargavguntupalli.tradingsandboxapi.models.Role;
import org.bhargavguntupalli.tradingsandboxapi.models.RoleEntity;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.repositories.RoleRepository;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(
            RoleRepository roleRepo,
            UserRepository userRepo,
            PasswordEncoder encoder,
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password}") String adminPassword,
            @Value("${admin.email}") String adminEmail,
            @Value("${admin.first-name}") String adminFirstName,
            @Value("${admin.last-name}") String adminLastName
    ) {
        return args -> {
            // seed ROLE_USER and ROLE_ADMIN
            for (Role r : Role.values()) {
                roleRepo.findByName(r)
                        .orElseGet(() -> roleRepo.save(new RoleEntity(r)));
            }

            // now seed the admin user
            if (userRepo.findByUsername(adminUsername).isEmpty()) {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(encoder.encode(adminPassword));
                admin.setEmail(adminEmail);
                admin.setFirstName(adminFirstName);
                admin.setLastName(adminLastName);

                // fetch the RoleEntity for ROLE_ADMIN
                RoleEntity adminRole = roleRepo.findByName(Role.ROLE_ADMIN)
                        .orElseThrow();
                admin.setRole(adminRole);   // ← use setRole, not setRoles

                userRepo.save(admin);
                System.out.println("Admin created --> " + adminUsername);
            }
        };
    }
}
