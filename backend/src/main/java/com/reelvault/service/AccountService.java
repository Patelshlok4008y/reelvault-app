package com.reelvault.service;

import com.reelvault.domain.AppUser;
import com.reelvault.repository.AppUserRepository;
import com.reelvault.repository.MediaRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final MediaRepository media;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository users, MediaRepository media, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.media = media;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(String email, String displayName, String password) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByNormalizedEmail(normalized)) {
            throw new AccountAlreadyExistsException();
        }
        boolean firstAccount = users.count() == 0;
        AppUser saved = users.save(new AppUser(normalized, displayName.trim(), passwordEncoder.encode(password)));
        if (firstAccount) {
            media.claimLegacyLibrary(saved);
        }
        return saved;
    }
}
