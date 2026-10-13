package com.reelvault.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    private int seasonNumber;
    private int episodeNumber;
    private String title;
    private Integer runtimeMinutes;
    private boolean watched;

    protected Episode() {
        // Required by JPA.
    }

    Episode(Series series, int seasonNumber, int episodeNumber, String title, Integer runtimeMinutes) {
        if (seasonNumber < 1 || episodeNumber < 1) {
            throw new IllegalArgumentException("Season and episode numbers must be positive.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Episode title is required.");
        }
        this.series = series;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.title = title.trim();
        this.runtimeMinutes = runtimeMinutes;
    }

    public void markWatched(boolean watched) { this.watched = watched; }
    public Long getId() { return id; }
    public int getSeasonNumber() { return seasonNumber; }
    public int getEpisodeNumber() { return episodeNumber; }
    public String getTitle() { return title; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public boolean isWatched() { return watched; }
    public Series getSeries() { return series; }
}
