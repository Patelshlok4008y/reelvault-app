package com.reelvault.dto;

import com.reelvault.domain.MediaType;
import com.reelvault.domain.CatalogueCategory;
import com.reelvault.domain.WatchStatus;
import java.time.Instant;
import java.util.List;

public record MediaResponse(
        Long id,
        MediaType mediaType,
        String title,
        Integer releaseYear,
        String description,
        List<String> genres,
        String posterUrl,
        String backdropUrl,
        String provider,
        String externalProviderId,
        Double externalRating,
        CatalogueCategory catalogueCategory,
        Integer runtimeMinutes,
        Integer personalRating,
        String review,
        boolean favourite,
        boolean watchlist,
        WatchStatus watchStatus,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt,
        List<EpisodeResponse> episodes
) { }
