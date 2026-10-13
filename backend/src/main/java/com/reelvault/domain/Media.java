package com.reelvault.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Shared aggregate root for every item in a personal library. Subclasses supply
 * their own type and progress calculation, demonstrating runtime polymorphism.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer releaseYear;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private String genres = "";

    private String posterUrl;
    private String backdropUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private String provider;
    private String externalProviderId;
    private Double externalRating;

    @Enumerated(EnumType.STRING)
    private CatalogueCategory catalogueCategory;
    private Integer personalRating;

    @Column(length = 4000)
    private String review;

    private boolean favourite;
    private boolean watchlist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchStatus watchStatus = WatchStatus.PLAN_TO_WATCH;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Media() {
        // Required by JPA.
    }

    protected Media(String title, Integer releaseYear, String description, String genres) {
        updateCoreDetails(title, releaseYear, description, genres, null, null);
    }

    public abstract MediaType getMediaType();

    /** Each kind of media expresses viewing progress in its own meaningful way. */
    public abstract int getProgressPercent();

    public void updateCoreDetails(String title, Integer releaseYear, String description, String genres,
                                  String posterUrl, String backdropUrl) {
        this.title = requireText(title, "Title");
        if (releaseYear == null || releaseYear < 1888 || releaseYear > 2100) {
            throw new IllegalArgumentException("Release year must be between 1888 and 2100.");
        }
        this.releaseYear = releaseYear;
        this.description = description == null ? "" : description.trim();
        this.genres = genres == null ? "" : genres.trim();
        this.posterUrl = emptyToNull(posterUrl);
        this.backdropUrl = emptyToNull(backdropUrl);
    }

    public void updatePersonalData(WatchStatus status, Integer rating, Boolean clearRating, String review,
                                   Boolean favourite, Boolean watchlist) {
        if (status != null) {
            this.watchStatus = status;
        }
        if (Boolean.TRUE.equals(clearRating)) {
            this.personalRating = null;
        } else if (rating != null) {
            if (rating < 1 || rating > 10) {
                throw new IllegalArgumentException("Personal rating must be between 1 and 10.");
            }
            this.personalRating = rating;
        }
        if (review != null) {
            this.review = review.trim();
        }
        if (favourite != null) {
            this.favourite = favourite;
        }
        if (watchlist != null) {
            this.watchlist = watchlist;
        }
    }

    public void changeStatus(WatchStatus status) {
        this.watchStatus = Objects.requireNonNull(status, "Watch status is required.");
    }

    public void setOwner(AppUser owner) { this.owner = owner; }
    public AppUser getOwner() { return owner; }
    public void setCatalogueSource(String provider, String externalProviderId, Double externalRating) {
        this.provider = provider;
        this.externalProviderId = externalProviderId;
        this.externalRating = externalRating;
    }
    public void setCatalogueCategory(CatalogueCategory category) { this.catalogueCategory = category; }
    public CatalogueCategory getCatalogueCategory() { return catalogueCategory; }
    public String getProvider() { return provider; }
    public String getExternalProviderId() { return externalProviderId; }
    public Double getExternalRating() { return externalRating; }

    public List<String> getGenreList() {
        return Arrays.stream(genres.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getDescription() { return description; }
    public String getGenres() { return genres; }
    public String getPosterUrl() { return posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public Integer getPersonalRating() { return personalRating; }
    public String getReview() { return review; }
    public boolean isFavourite() { return favourite; }
    public boolean isWatchlist() { return watchlist; }
    public WatchStatus getWatchStatus() { return watchStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
