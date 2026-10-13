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
public class AniListCatalogueProvider implements CatalogueProvider {
    private static final String GRAPHQL = "https://graphql.anilist.co";
    private static final String FIELDS = "id title { romaji english native } description(asHtml:false) coverImage { large extraLarge } bannerImage genres averageScore episodes duration format season seasonYear startDate { year }";
    private final RestClient client = RestClient.builder().baseUrl(GRAPHQL).requestFactory(requestFactory()).build();

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(7000);
        return factory;
    }

    @Override public String provider() { return "ANILIST"; }

    @Override public CataloguePage search(CatalogueCategory category, String query, int page) {
        boolean movie = category == CatalogueCategory.ANIME_MOVIE;
        String format = movie ? "MOVIE" : "TV,TV_SHORT,ONA,OVA,SPECIAL";
        String gql = "query($page:Int,$perPage:Int,$search:String,$formats:[MediaFormat]){ Page(page:$page,perPage:$perPage){ pageInfo { total currentPage lastPage perPage } media(search:$search,type:ANIME,format_in:$formats,sort:POPULARITY_DESC){ " + FIELDS + " } } }";
        JsonNode pageNode = query(gql, page, query, format);
        JsonNode info = pageNode.path("pageInfo");
        List<CatalogueTitle> titles = new ArrayList<>();
        pageNode.path("media").forEach(n -> titles.add(map(category, n)));
        return new CataloguePage(info.path("currentPage").asInt(page), info.path("perPage").asInt(20),
                info.path("total").asInt(titles.size()), info.path("lastPage").asInt(0), titles);
    }

    @Override public CatalogueTitle details(CatalogueCategory category, String externalId) {
        String gql = "query($id:Int){ Media(id:$id,type:ANIME){ " + FIELDS + " } }";
        int id;
        try { id = Integer.parseInt(externalId); } catch (NumberFormatException e) { throw new CatalogueProviderException(HttpStatus.BAD_REQUEST, "AniList ID must be numeric."); }
        JsonNode vars = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("id", id);
        JsonNode response;
        try {
            response = client.post().body(java.util.Map.of("query", gql, "variables", vars)).retrieve().body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 429) throw new CatalogueProviderException(HttpStatus.TOO_MANY_REQUESTS, "AniList is rate limiting requests. Please wait and try again.");
            if (exception.getStatusCode().value() == 404) throw new CatalogueProviderException(HttpStatus.NOT_FOUND, "AniList title was not found.");
            throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList is temporarily unavailable.");
        } catch (Exception exception) { throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList could not be reached. Check the network and try again."); }
        if (response != null && response.has("errors")) throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList returned a catalogue error.");
        JsonNode node = response == null ? null : response.path("data").path("Media");
        if (node == null || node.isMissingNode() || node.isNull()) throw new CatalogueProviderException(HttpStatus.NOT_FOUND, "AniList title was not found.");
        return map(category, node);
    }

    private JsonNode query(String gql, int page, String search, String format) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        var vars = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                .put("page", page).put("perPage", 20);
        var formats = vars.putArray("formats");
        for (String value : format.split(",")) formats.add(value);
        if (normalizedSearch == null) vars.putNull("search"); else vars.put("search", normalizedSearch);
        try {
            JsonNode response = client.post().body(java.util.Map.of("query", gql, "variables", vars)).retrieve().body(JsonNode.class);
            if (response != null && response.has("errors")) {
                String message = response.path("errors").path(0).path("message").asText("");
                if (message.toLowerCase().contains("rate") || message.contains("429")) throw new CatalogueProviderException(HttpStatus.TOO_MANY_REQUESTS, "AniList is rate limiting requests. Please wait and try again.");
                throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList returned a catalogue error.");
            }
            return response.path("data").path("Page");
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 429) throw new CatalogueProviderException(HttpStatus.TOO_MANY_REQUESTS, "AniList is rate limiting requests. Please wait and try again.");
            throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList is temporarily unavailable.");
        } catch (CatalogueProviderException exception) { throw exception; }
        catch (Exception exception) { throw new CatalogueProviderException(HttpStatus.BAD_GATEWAY, "AniList could not be reached. Check the network and try again."); }
    }

    private CatalogueTitle map(CatalogueCategory category, JsonNode n) {
        String title = firstText(n.path("title"), "english", "romaji", "native");
        List<String> genres = new ArrayList<>(); n.path("genres").forEach(g -> genres.add(g.asText()));
        JsonNode cover = n.path("coverImage");
        String poster = firstImage(cover.path("extraLarge"), cover.path("large"));
        Integer year = n.path("startDate").path("year").isNumber() ? n.path("startDate").path("year").asInt() : null;
        Double score = n.path("averageScore").isNumber() ? n.path("averageScore").asDouble() / 10.0 : null;
        return new CatalogueTitle("ANILIST", n.path("id").asText(), category,
                category == CatalogueCategory.ANIME_MOVIE ? "MOVIE" : "SERIES", title, year,
                n.path("description").asText(""), genres, poster, n.path("bannerImage").asText(null), score,
                n.path("duration").isNumber() ? n.path("duration").asInt() : null,
                n.path("episodes").isNumber() ? n.path("episodes").asInt() : null, null,
                n.path("format").asText(null), n.path("season").asText(null),
                category == CatalogueCategory.ANIME_SERIES && n.path("episodes").isNumber() && n.path("episodes").asInt() > 0
                        ? List.of(new CatalogueTitle.SeasonSummary(1, Math.min(n.path("episodes").asInt(), 2000))) : List.of());
    }
    private String firstImage(JsonNode... values) { for (JsonNode value : values) { String url = value.asText(""); if (!url.isBlank()) return url; } return null; }
    private String firstText(JsonNode node, String... fields) { for (String field : fields) { String v = node.path(field).asText(""); if (!v.isBlank()) return v; } return "Untitled"; }
}
