package com.reelvault.dto;

import com.reelvault.domain.CatalogueCategory;
import java.util.List;

public record CatalogueTitle(
        String provider, String externalId, CatalogueCategory category, String mediaType,
        String title, Integer year, String description, List<String> genres,
        String posterUrl, String backdropUrl, Double rating, Integer runtimeMinutes,
        Integer episodeCount, Integer seasonCount, String format, String season,
        List<SeasonSummary> seasons
) {
    public record SeasonSummary(int seasonNumber, int episodeCount) { }
}
