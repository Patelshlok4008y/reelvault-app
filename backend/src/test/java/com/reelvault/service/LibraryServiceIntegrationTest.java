package com.reelvault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reelvault.domain.MediaType;
import com.reelvault.domain.WatchStatus;
import com.reelvault.domain.AppUser;
import com.reelvault.dto.EpisodeRequest;
import com.reelvault.dto.MediaRequest;
import com.reelvault.dto.MediaResponse;
import com.reelvault.dto.PersonalUpdateRequest;
import com.reelvault.dto.CatalogueTitle;
import com.reelvault.domain.CatalogueCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.reelvault.security.ReelvaultUserPrincipal;
import com.reelvault.repository.AppUserRepository;
import com.reelvault.repository.EpisodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:reelvault-test?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext
@Transactional
class LibraryServiceIntegrationTest {
    @Autowired
    private LibraryService libraryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired private AppUserRepository users;
    @Autowired private EpisodeRepository episodes;
    @Autowired private PasswordEncoder encoder;
    private AppUser firstUser;

    @BeforeEach
    void authenticateTestUser() {
        firstUser = users.save(new AppUser("first@example.com", "First User", encoder.encode("test-password")));
        authenticate(firstUser);
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void authenticate(AppUser user) {
        ReelvaultUserPrincipal principal = ReelvaultUserPrincipal.from(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void savesFiltersAndReloadsMoviePersonalData() {
        MediaResponse created = libraryService.createMedia(new MediaRequest(MediaType.MOVIE, "Verification Film", 2024,
                "A test item that should remain available after the persistence context is cleared.", "Drama, Mystery", null, null, 101));
        libraryService.updatePersonalData(created.id(), new PersonalUpdateRequest(WatchStatus.WATCHING, 8, false,
                "Strong atmosphere.", true, true));

        assertThat(libraryService.findMedia("verification", MediaType.MOVIE, "Drama", WatchStatus.WATCHING, "rating"))
                .extracting(MediaResponse::title)
                .contains("Verification Film");

        entityManager.flush();
        entityManager.clear();
        MediaResponse reloaded = libraryService.getMedia(created.id());
        assertThat(reloaded.personalRating()).isEqualTo(8);
        assertThat(reloaded.review()).isEqualTo("Strong atmosphere.");
        assertThat(reloaded.favourite()).isTrue();
    }

    @Test
    void calculatesSeriesEpisodeProgressAndValidatesInput() {
        MediaResponse series = libraryService.createMedia(new MediaRequest(MediaType.SERIES, "Progress Series", 2025,
                "Testing episode state.", "Science Fiction", null, null, null));
        libraryService.addEpisode(series.id(), new EpisodeRequest(1, 1, "Start", 45));
        MediaResponse withTwoEpisodes = libraryService.addEpisode(series.id(), new EpisodeRequest(1, 2, "Finish", 45));

        MediaResponse watching = libraryService.updateEpisodeWatched(withTwoEpisodes.episodes().get(0).id(), true);
        assertThat(watching.progressPercent()).isEqualTo(50);
        assertThat(watching.watchStatus()).isEqualTo(WatchStatus.WATCHING);

        MediaResponse completed = libraryService.updateEpisodeWatched(withTwoEpisodes.episodes().get(1).id(), true);
        assertThat(completed.progressPercent()).isEqualTo(100);
        assertThat(completed.watchStatus()).isEqualTo(WatchStatus.COMPLETED);
        MediaResponse reopened = libraryService.updateEpisodeWatched(completed.episodes().get(1).id(), false);
        assertThat(reopened.progressPercent()).isEqualTo(50);
        assertThat(reopened.watchStatus()).isEqualTo(WatchStatus.WATCHING);

        assertThatThrownBy(() -> libraryService.createMedia(new MediaRequest(MediaType.MOVIE, "", 1700,
                "", "", null, null, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importsRealProviderEpisodeCountsAndDeletionPersistsForOwner() {
        CatalogueTitle catalogue = new CatalogueTitle("TMDB", "555", CatalogueCategory.TV_SERIES, "SERIES",
                "Imported Series", 2024, "Synopsis", java.util.List.of("Drama"), null, null, 8.2, 45,
                5, 2, null, null, java.util.List.of(new CatalogueTitle.SeasonSummary(1, 2),
                new CatalogueTitle.SeasonSummary(2, 3)));
        MediaResponse imported = libraryService.importCatalogueTitle(catalogue);
        assertThat(imported.episodes()).hasSize(5);
        assertThat(imported.progressPercent()).isZero();
        libraryService.deleteMedia(imported.id());
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> libraryService.getMedia(imported.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(episodes.countBySeriesId(imported.id())).isZero();
    }

    @Test
    void anotherAccountCannotReadOrUpdateLibraryTitlesOrEpisodes() {
        MediaResponse movie = libraryService.createMedia(new MediaRequest(MediaType.MOVIE, "Private Film", 2024,
                "Private synopsis", "Drama", null, null, 110));
        MediaResponse series = libraryService.createMedia(new MediaRequest(MediaType.SERIES, "Private Series", 2024,
                "Private synopsis", "Drama", null, null, null));
        MediaResponse withEpisode = libraryService.addEpisode(series.id(), new EpisodeRequest(1, 1, "Private Episode", 45));
        Long episodeId = withEpisode.episodes().get(0).id();

        AppUser other = users.save(new AppUser("other@example.com", "Other User", encoder.encode("test-password")));
        authenticate(other);

        assertThat(libraryService.findMedia(null, null, null, null, "recent")).isEmpty();
        assertThatThrownBy(() -> libraryService.getMedia(movie.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> libraryService.updateEpisodeWatched(episodeId, true)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> libraryService.deleteMedia(movie.id())).isInstanceOf(ResourceNotFoundException.class);
        authenticate(firstUser);
        assertThat(libraryService.getMedia(movie.id()).title()).isEqualTo("Private Film");
    }
}
