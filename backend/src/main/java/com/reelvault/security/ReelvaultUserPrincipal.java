package com.reelvault.security;

import com.reelvault.domain.AppUser;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record ReelvaultUserPrincipal(long id, String email, String displayName, String passwordHash, Instant createdAt) implements UserDetails {
    public static ReelvaultUserPrincipal from(AppUser user) {
        return new ReelvaultUserPrincipal(user.getId(), user.getNormalizedEmail(), user.getDisplayName(), user.getPasswordHash(), user.getCreatedAt());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
