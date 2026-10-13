package com.reelvault.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
public class Series extends Media {
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Episode> episodes = new ArrayList<>();

    protected Series() {
        // Required by JPA.
    }

    public Series(String title, Integer releaseYear, String description, String genres) {
        super(title, releaseYear, description, genres);
        setCatalogueCategory(CatalogueCategory.TV_SERIES);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.SERIES;
    }

    @Override
    public int getProgressPercent() {
        if (episodes.isEmpty()) {
            return getWatchStatus() == WatchStatus.COMPLETED ? 100 : 0;
        }
        long watched = episodes.stream().filter(Episode::isWatched).count();
        return (int) Math.round((watched * 100.0) / episodes.size());
    }

    public Episode addEpisode(int seasonNumber, int episodeNumber, String title, Integer runtimeMinutes) {
        boolean duplicate = episodes.stream().anyMatch(episode ->
                episode.getSeasonNumber() == seasonNumber && episode.getEpisodeNumber() == episodeNumber);
        if (duplicate) {
            throw new IllegalArgumentException("That season and episode number already exists.");
        }
        Episode episode = new Episode(this, seasonNumber, episodeNumber, title, runtimeMinutes);
        episodes.add(episode);
        return episode;
    }

    public List<Episode> getEpisodes() {
        return episodes.stream()
                .sorted(Comparator.comparingInt(Episode::getSeasonNumber)
                        .thenComparingInt(Episode::getEpisodeNumber))
                .toList();
    }
}
