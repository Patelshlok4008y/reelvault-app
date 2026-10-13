package com.reelvault.security;

import com.reelvault.repository.AppUserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ReelvaultUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public ReelvaultUserDetailsService(AppUserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return users.findByNormalizedEmail(normalized)
                .map(ReelvaultUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Account was not found."));
    }
}
