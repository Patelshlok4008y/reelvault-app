package com.reelvault.service;

import com.reelvault.domain.Episode;
import com.reelvault.domain.AppUser;
import com.reelvault.domain.Media;
import com.reelvault.domain.MediaType;
import com.reelvault.domain.Movie;
import com.reelvault.domain.Series;
import com.reelvault.domain.WatchStatus;
import com.reelvault.dto.DashboardResponse;
import com.reelvault.dto.EpisodeRequest;
import com.reelvault.dto.EpisodeResponse;
import com.reelvault.dto.LibraryStatsResponse;
import com.reelvault.dto.MediaRequest;
import com.reelvault.dto.MediaResponse;
import com.reelvault.dto.CatalogueTitle;
import com.reelvault.dto.PersonalUpdateRequest;
import com.reelvault.repository.EpisodeRepository;
import com.reelvault.repository.MediaRepository;
import com.reelvault.security.ReelvaultUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates library operations and keeps HTTP concerns out of the domain model. */
@Service
@Transactional(readOnly = true)
public class LibraryService {
    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final com.reelvault.repository.AppUserRepository userRepository;

    public LibraryService(MediaRepository mediaRepository, EpisodeRepository episodeRepository,
                          com.reelvault.repository.AppUserRepository userRepository) {
        this.mediaRepository = mediaRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
    }

    public List<MediaResponse> findMedia(String search, MediaType type, String genre, WatchStatus status, String sort) {
        Comparator<Media> comparator = switch (sort == null ? "recent" : sort.toLowerCase(Locale.ROOT)) {
            case "rating" -> Comparator.comparing(Media::getPersonalRating,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "year" -> Comparator.comparing(Media::getReleaseYear, Comparator.reverseOrder());
            case "title" -> Comparator.comparing(Media::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Media::getCreatedAt, Comparator.reverseOrder());
        };

        return mediaRepository.findAllByOwnerId(currentUserId()).stream()
                .filter(media -> containsIgnoreCase(media.getTitle(), search))
                .filter(media -> type == null || media.getMediaType() == type)
                .filter(media -> status == null || media.getWatchStatus() == status)
                .filter(media -> genre == null || genre.isBlank() || media.getGenreList().stream()
                        .anyMatch(item -> item.equalsIgnoreCase(genre.trim())))
                .sorted(comparator.thenComparing(Media::getId))
                .map(this::toResponse)
                .toList();
    }

    public MediaResponse getMedia(long id) {
        return toResponse(getMediaEntity(id));
    }

    @Transactional
    public MediaResponse createMedia(MediaRequest request) {
        Media media = request.mediaType() == MediaType.MOVIE
                ? new Movie(request.title(), request.releaseYear(), request.description(), request.genres(), request.runtimeMinutes())
                : new Series(request.title(), request.releaseYear(), request.description(), request.genres());
        media.updateCoreDetails(request.title(), request.releaseYear(), request.description(), request.genres(),
                request.posterUrl(), request.backdropUrl());
        media.setOwner(currentUser());
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    public MediaResponse importCatalogueTitle(CatalogueTitle title) {
        Long ownerId = currentUserId();
        var existing = mediaRepository.findByOwnerIdAndProviderAndExternalProviderId(ownerId, title.provider(), title.externalId());
        if (existing.isPresent()) {
            if (existing.get() instanceof Series series && series.getEpisodes().isEmpty()) addCatalogueEpisodes(series, title);
            return toResponse(existing.get());
        }
        Media media = "MOVIE".equals(title.mediaType())
                ? new Movie(title.title(), safeYear(title.year()), title.description(), String.join(", ", title.genres()), title.runtimeMinutes())
                : new Series(title.title(), safeYear(title.year()), title.description(), String.join(", ", title.genres()));
        media.updateCoreDetails(title.title(), safeYear(title.year()), title.description(), String.join(", ", title.genres()),
                title.posterUrl(), title.backdropUrl());
        media.setOwner(currentUser());
        media.setCatalogueSource(title.provider(), title.externalId(), title.rating());
        media.setCatalogueCategory(title.category());
        if (media instanceof Series series) addCatalogueEpisodes(series, title);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    public MediaResponse synchronizeCatalogueEpisodes(long seriesId, CatalogueTitle title) {
        Series series = getSeries(seriesId);
        if (!Objects.equals(series.getProvider(), title.provider())
                || !Objects.equals(series.getExternalProviderId(), title.externalId())) {
            throw new IllegalArgumentException("Catalogue metadata does not match this series.");
        }
        addCatalogueEpisodes(series, title);
        return toResponse(mediaRepository.save(series));
    }

    private void addCatalogueEpisodes(Series series, CatalogueTitle title) {
        if (title.seasons() == null) return;
        Set<String> existing = new HashSet<>();
        series.getEpisodes().forEach(episode -> existing.add(episode.getSeasonNumber() + ":" + episode.getEpisodeNumber()));
        for (CatalogueTitle.SeasonSummary season : title.seasons()) {
            if (season.seasonNumber() < 1) continue;
            int count = Math.max(0, Math.min(season.episodeCount(), 2000));
            for (int number = 1; number <= count; number++) {
                if (existing.add(season.seasonNumber() + ":" + number)) {
                    series.addEpisode(season.seasonNumber(), number, "Episode " + number, null);
                }
            }
        }
    }

    private int safeYear(Integer year) {
        if (year == null || year < 1888 || year > 2100) {
            throw new IllegalArgumentException("This catalogue title has no valid release year, so it cannot be added without inventing metadata.");
        }
        return year;
    }

    @Transactional
    public MediaResponse updateMedia(long id, MediaRequest request) {
        Media media = getMediaEntity(id);
        if (media.getMediaType() != request.mediaType()) {
            throw new IllegalArgumentException("A title's media type cannot be changed after creation.");
        }
        media.updateCoreDetails(request.title(), request.releaseYear(), request.description(), request.genres(),
                request.posterUrl(), request.backdropUrl());
        if (media instanceof Movie movie) {
            movie.setRuntimeMinutes(request.runtimeMinutes());
        }
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    public MediaResponse updatePersonalData(long id, PersonalUpdateRequest request) {
        Media media = getMediaEntity(id);
        media.updatePersonalData(request.watchStatus(), request.personalRating(), request.clearRating(), request.review(),
                request.favourite(), request.watchlist());
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    public MediaResponse updateStatus(long id, WatchStatus status) {
        Media media = getMediaEntity(id);
        media.changeStatus(status);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    public void deleteMedia(long id) {
        mediaRepository.delete(getMediaEntity(id));
    }

    @Transactional
    public MediaResponse addEpisode(long seriesId, EpisodeRequest request) {
        Series series = getSeries(seriesId);
        series.addEpisode(request.seasonNumber(), request.episodeNumber(), request.title(), request.runtimeMinutes());
        return toResponse(mediaRepository.save(series));
    }

    @Transactional
    public MediaResponse updateEpisodeWatched(long episodeId, boolean watched) {
        Episode episode = episodeRepository.findByIdAndSeriesOwnerId(episodeId, currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Episode " + episodeId + " was not found."));
        episode.markWatched(watched);
        episodeRepository.save(episode);
        Series series = episode.getSeries();
        long watchedCount = series.getEpisodes().stream().filter(Episode::isWatched).count();
        if (watchedCount == series.getEpisodes().size() && watchedCount > 0) {
            series.changeStatus(WatchStatus.COMPLETED);
        } else if (watchedCount > 0) {
            series.changeStatus(WatchStatus.WATCHING);
        } else if (series.getWatchStatus() == WatchStatus.COMPLETED ||
                (watchedCount == 0 && series.getWatchStatus() == WatchStatus.WATCHING)) {
            series.changeStatus(WatchStatus.PLAN_TO_WATCH);
        }
        return toResponse(mediaRepository.save(series));
    }

    public DashboardResponse getDashboard() {
        List<Media> all = new ArrayList<>(mediaRepository.findAllByOwnerId(currentUserId()));
        List<Media> recent = all.stream().sorted(Comparator.comparing(Media::getCreatedAt).reversed()).toList();
        List<Media> watching = all.stream()
                .filter(media -> media.getWatchStatus() == WatchStatus.WATCHING)
                .sorted(Comparator.comparing(Media::getUpdatedAt).reversed())
                .toList();
        Media featured = all.stream().filter(Media::isFavourite)
                .max(Comparator.comparing(Media::getUpdatedAt))
                .orElse(recent.isEmpty() ? null : recent.get(0));

        LibraryStatsResponse stats = new LibraryStatsResponse(
                all.size(),
                all.stream().filter(media -> media.getMediaType() == MediaType.MOVIE).count(),
                all.stream().filter(media -> media.getMediaType() == MediaType.SERIES).count(),
                all.stream().filter(media -> media.getWatchStatus() == WatchStatus.COMPLETED).count(),
                all.stream().filter(Media::isFavourite).count()
        );
        return new DashboardResponse(featured == null ? null : toResponse(featured), stats,
                watching.stream().limit(8).map(this::toResponse).toList(),
                recent.stream().limit(8).map(this::toResponse).toList());
    }

    public long count() {
        return mediaRepository.countByOwnerId(currentUserId());
    }

    private Media getMediaEntity(long id) {
        return mediaRepository.findByIdAndOwnerId(id, currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Media item " + id + " was not found."));
    }

    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ReelvaultUserPrincipal principal)) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return userRepository.findById(principal.id()).orElseThrow(() -> new AccessDeniedException("Authentication is required."));
    }

    private Long currentUserId() { return currentUser().getId(); }

    private Series getSeries(long id) {
        Media media = getMediaEntity(id);
        if (!(media instanceof Series series)) {
            throw new IllegalArgumentException("Only a series can have episodes.");
        }
        return series;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return query == null || query.isBlank() || value.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private MediaResponse toResponse(Media media) {
        List<EpisodeResponse> episodes = media instanceof Series series
                ? series.getEpisodes().stream().map(episode -> new EpisodeResponse(episode.getId(), episode.getSeasonNumber(),
                episode.getEpisodeNumber(), episode.getTitle(), episode.getRuntimeMinutes(), episode.isWatched())).toList()
                : List.of();
        Integer runtime = media instanceof Movie movie ? movie.getRuntimeMinutes() : null;
        return new MediaResponse(media.getId(), media.getMediaType(), media.getTitle(), media.getReleaseYear(),
                media.getDescription(), media.getGenreList(), media.getPosterUrl(), media.getBackdropUrl(),
                media.getProvider(), media.getExternalProviderId(), media.getExternalRating(), media.getCatalogueCategory(), runtime,
                media.getPersonalRating(), media.getReview(), media.isFavourite(), media.isWatchlist(),
                media.getWatchStatus(), media.getProgressPercent(), media.getCreatedAt(), media.getUpdatedAt(), episodes);
    }
}
