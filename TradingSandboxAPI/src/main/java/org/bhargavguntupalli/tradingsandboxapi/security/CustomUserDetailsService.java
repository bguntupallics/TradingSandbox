package org.bhargavguntupalli.tradingsandboxapi.security;

import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.bhargavguntupalli.tradingsandboxapi.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) { this.repo = repo; }

    /**
     * Loads user by email (used as login credential).
     * The "username" parameter is actually the user's email.
     * Users who haven't verified their email are marked as disabled.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPassword(),
                u.isEmailVerified(),  // enabled
                true,                 // accountNonExpired
                true,                 // credentialsNonExpired
                true,                 // accountNonLocked
                List.of(new SimpleGrantedAuthority(u.getRole().getName().name()))
        );
    }
}
