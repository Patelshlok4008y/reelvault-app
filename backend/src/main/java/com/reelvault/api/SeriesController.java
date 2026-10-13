package com.reelvault.api;

import com.reelvault.dto.EpisodeRequest;
import com.reelvault.dto.EpisodeWatchedRequest;
import com.reelvault.dto.MediaResponse;
import com.reelvault.service.LibraryService;
import com.reelvault.service.CatalogueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SeriesController {
    private final LibraryService libraryService;
    private final CatalogueService catalogueService;

    public SeriesController(LibraryService libraryService, CatalogueService catalogueService) {
        this.libraryService = libraryService;
        this.catalogueService = catalogueService;
    }

    @PostMapping("/series/{seriesId}/episodes")
    public MediaResponse addEpisode(@PathVariable long seriesId, @Valid @RequestBody EpisodeRequest request) {
        return libraryService.addEpisode(seriesId, request);
    }

    @PostMapping("/series/{seriesId}/episodes/sync")
    public MediaResponse syncEpisodes(@PathVariable long seriesId) {
        MediaResponse media = libraryService.getMedia(seriesId);
        if (media.provider() == null || media.externalProviderId() == null || media.catalogueCategory() == null) {
            throw new IllegalArgumentException("Episode metadata is unavailable for this series. Add episodes manually.");
        }
        return libraryService.synchronizeCatalogueEpisodes(seriesId,
                catalogueService.details(media.provider(), media.catalogueCategory(), media.externalProviderId()));
    }

    @PatchMapping("/episodes/{episodeId}/watched")
    public MediaResponse updateEpisode(@PathVariable long episodeId, @Valid @RequestBody EpisodeWatchedRequest request) {
        return libraryService.updateEpisodeWatched(episodeId, request.watched());
    }
}
