package com.reelvault.domain;

import jakarta.persistence.Entity;

@Entity
public class Movie extends Media {
    private Integer runtimeMinutes;

    protected Movie() {
        // Required by JPA.
    }

    public Movie(String title, Integer releaseYear, String description, String genres, Integer runtimeMinutes) {
        super(title, releaseYear, description, genres);
        setCatalogueCategory(CatalogueCategory.MOVIE);
        setRuntimeMinutes(runtimeMinutes);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.MOVIE;
    }

    @Override
    public int getProgressPercent() {
        return switch (getWatchStatus()) {
            case COMPLETED -> 100;
            case WATCHING -> 50;
            default -> 0;
        };
    }

    public void setRuntimeMinutes(Integer runtimeMinutes) {
        if (runtimeMinutes != null && (runtimeMinutes < 1 || runtimeMinutes > 1000)) {
            throw new IllegalArgumentException("Runtime must be between 1 and 1000 minutes.");
        }
        this.runtimeMinutes = runtimeMinutes;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }
}
