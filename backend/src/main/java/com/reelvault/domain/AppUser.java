package com.reelvault.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String normalizedEmail;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() { }

    public AppUser(String normalizedEmail, String displayName, String passwordHash) {
        this.normalizedEmail = normalizedEmail;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void initializeCreatedAt() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
