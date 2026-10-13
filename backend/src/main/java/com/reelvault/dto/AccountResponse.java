package com.reelvault.dto;

import com.reelvault.domain.AppUser;
import java.time.Instant;

public record AccountResponse(long id, String email, String displayName, Instant createdAt) {
    public static AccountResponse from(AppUser user) {
        return new AccountResponse(user.getId(), user.getNormalizedEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
