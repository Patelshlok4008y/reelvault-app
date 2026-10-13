package com.reelvault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.reelvault.domain.CatalogueCategory;
import com.reelvault.dto.CataloguePage;
import com.reelvault.dto.CatalogueTitle;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class TmdbCatalogueProvider implements CatalogueProvider {
    private static final String API = "https://api.themoviedb.org/3";
    private final String token;
    private final RestClient client;

    public TmdbCatalogueProvider() { this(System.getenv("TMDB_READ_ACCESS_TOKEN")); }

    TmdbCatalogueProvider(String token) {
        this.token = token;
        this.client = RestClient.builder().baseUrl(API).requestFactory(requestFactory()).build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(7000);
        return factory;
    }

    @Override public String provider() { return "TMDB"; }

    @Override public CataloguePage search(CatalogueCategory category, String query, int page) {
        ensureConfigured();
        String kind = category == CatalogueCategory.TV_SERIES ? "tv" : "movie";
        String path = query == null || query.isBlank()
                ? "/discover/" + kind + "?sort_by=popularity.desc&page=" + page
                : "/search/" + kind + "?query=" + encode(query) + "&page=" + page;
        JsonNode root = get(path);
        List<CatalogueTitle> results = new ArrayList<>();
        root.path("results").forEach(node -> results.add(map(category, node, false)));
        int totalPages = Math.min(root.path("total_pages").asInt(0), 500);
        return new CataloguePage(page, 20, root.path("total_results").asInt(results.size()), totalPages, results);
    }

    @Override public CatalogueTitle details(CatalogueCategory category, String externalId) {
        ensureConfigured();
        String kind = category == CatalogueCategory.TV_SERIES ? "tv" : "movie";
        JsonNode node = get("/" + kind + "/" + encode(externalId) + "?append_to_response=credits");
        return map(category, node, true);
    }

    private JsonNode get(String path) {
        try {
            return client.get().uri(path).header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json").retrieve().body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 429) throw new CatalogueProviderException(HttpStatus.TOO_MANY_REQUESTS, "TMDB is rate limiting requests. Please wait and try again.");
            if (exception.getStatusCode().is4xxClientError()) throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "TMDB rejected the catalogue request. Check the backend TMDB configuration.");
            throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "TMDB is temporarily unavailable.");
        } catch (Exception exception) {
            if (exception instanceof CatalogueProviderException providerException) throw providerException;
            throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "TMDB could not be reached. Check the network and try again.");
        }
    }

    private void ensureConfigured() {
        if (token == null || token.isBlank()) throw new CatalogueProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                "TMDB is not configured. Set TMDB_READ_ACCESS_TOKEN in the backend environment to search films and TV series.");
    }

    private CatalogueTitle map(CatalogueCategory category, JsonNode node, boolean detail) {
        boolean tv = category == CatalogueCategory.TV_SERIES;
        String title = text(node, tv ? "name" : "title");
        String date = text(node, tv ? "first_air_date" : "release_date");
        Integer year = date.length() >= 4 ? parseInt(date.substring(0, 4)) : null;
        List<String> genres = new ArrayList<>();
        JsonNode genreNodes = node.path("genres");
        if (genreNodes.isArray()) genreNodes.forEach(g -> genres.add(text(g, "name")));
        else node.path("genre_ids").forEach(id -> { String label = tmdbGenre(category, id.asInt()); if (label != null) genres.add(label); });
        String poster = image(node, "poster_path");
        String backdrop = image(node, "backdrop_path");
        Integer runtime = tv ? firstInt(node.path("episode_run_time")) : nullableInt(node, "runtime");
        List<CatalogueTitle.SeasonSummary> seasons = new ArrayList<>();
        if (tv && node.path("seasons").isArray()) node.path("seasons").forEach(s -> {
            int number = s.path("season_number").asInt(-1);
            int count = s.path("episode_count").asInt(0);
            if (number > 0 && count > 0) seasons.add(new CatalogueTitle.SeasonSummary(number, Math.min(count, 2000)));
        });
        if (tv && seasons.isEmpty() && nullableInt(node, "number_of_episodes") != null
                && nullableInt(node, "number_of_episodes") > 0) {
            seasons.add(new CatalogueTitle.SeasonSummary(1, Math.min(nullableInt(node, "number_of_episodes"), 2000)));
        }
        return new CatalogueTitle("TMDB", node.path("id").asText(), category, tv ? "SERIES" : "MOVIE",
                title, year, text(node, "overview"), genres, poster, backdrop,
                node.hasNonNull("vote_average") ? node.path("vote_average").asDouble() : null, runtime,
                tv ? nullableInt(node, "number_of_episodes") : null, tv ? nullableInt(node, "number_of_seasons") : null,
                null, null, seasons);
    }

    private String image(JsonNode n, String key) {
        String path = text(n, key);
        if (path.isBlank()) return null;
        return "https://image.tmdb.org/t/p/" + ("backdrop_path".equals(key) ? "w1280" : "w500") + path;
    }
    private String text(JsonNode n, String key) { return n.path(key).isNull() ? "" : n.path(key).asText(""); }
    private Integer nullableInt(JsonNode n, String key) { return n.hasNonNull(key) ? n.path(key).asInt() : null; }
    private Integer firstInt(JsonNode n) { return n.isArray() && !n.isEmpty() ? n.get(0).asInt() : null; }
    private Integer parseInt(String v) { try { return Integer.valueOf(v); } catch (NumberFormatException e) { return null; } }
    private String encode(String value) { return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8); }
    private String tmdbGenre(CatalogueCategory category, int id) {
        if (category == CatalogueCategory.TV_SERIES) return switch (id) {
            case 10759 -> "Action & Adventure"; case 16 -> "Animation"; case 35 -> "Comedy"; case 80 -> "Crime";
            case 99 -> "Documentary"; case 18 -> "Drama"; case 10751 -> "Family"; case 10762 -> "Kids";
            case 9648 -> "Mystery"; case 10763 -> "News"; case 10764 -> "Reality"; case 10765 -> "Sci-Fi & Fantasy";
            case 10766 -> "Soap"; case 10767 -> "Talk"; case 10768 -> "War & Politics"; case 37 -> "Western"; default -> null;
        };
        return switch (id) {
            case 28 -> "Action"; case 12 -> "Adventure"; case 16 -> "Animation"; case 35 -> "Comedy";
            case 80 -> "Crime"; case 99 -> "Documentary"; case 18 -> "Drama"; case 10751 -> "Family";
            case 14 -> "Fantasy"; case 36 -> "History"; case 27 -> "Horror"; case 10402 -> "Music";
            case 9648 -> "Mystery"; case 10749 -> "Romance"; case 878 -> "Science Fiction";
            case 10770 -> "TV Movie"; case 53 -> "Thriller"; case 10752 -> "War"; case 37 -> "Western"; default -> null;
        };
    }
}
