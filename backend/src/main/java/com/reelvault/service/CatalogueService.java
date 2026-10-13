package com.reelvault.service;

import com.reelvault.domain.CatalogueCategory;
import com.reelvault.dto.CataloguePage;
import com.reelvault.dto.CatalogueTitle;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService {
    private record Cached(Instant expiresAt, CataloguePage page) { }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, CatalogueProvider> providers;

    public CatalogueService(java.util.List<CatalogueProvider> available) {
        this.providers = available.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(p -> p.provider().toUpperCase(), p -> p));
    }

    public CataloguePage search(String provider, CatalogueCategory category, String query, int page) {
        if (page < 1 || page > 500) throw new IllegalArgumentException("Page must be between 1 and 500.");
        String normalized = provider(provider, category);
        String key = normalized + "|" + category + "|" + (query == null ? "" : query.trim().toLowerCase()) + "|" + page;
        Cached hit = cache.get(key);
        if (hit != null && hit.expiresAt().isAfter(Instant.now())) return hit.page();
        CataloguePage response = providers.get(normalized).search(category, query, page);
        if (cache.size() > 500) cache.clear();
        cache.put(key, new Cached(Instant.now().plusSeconds(300), response));
        return response;
    }

    public CatalogueTitle details(String provider, CatalogueCategory category, String externalId) {
        return providers.get(provider(provider, category)).details(category, externalId);
    }

    private String provider(String supplied, CatalogueCategory category) {
        String expected = category == CatalogueCategory.ANIME_MOVIE || category == CatalogueCategory.ANIME_SERIES ? "ANILIST" : "TMDB";
        String normalized = supplied == null || supplied.isBlank() ? expected : supplied.trim().toUpperCase();
        if (!expected.equals(normalized) || !providers.containsKey(normalized)) throw new IllegalArgumentException("The selected provider does not support this catalogue category.");
        return normalized;
    }
}
