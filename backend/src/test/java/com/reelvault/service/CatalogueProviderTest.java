package com.reelvault.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reelvault.domain.CatalogueCategory;
import org.junit.jupiter.api.Test;

class CatalogueProviderTest {
    @Test
    void tmdbWithoutRuntimeTokenFailsClearlyInsteadOfReturningSampleResults() {
        TmdbCatalogueProvider provider = new TmdbCatalogueProvider("");
        assertThatThrownBy(() -> provider.search(CatalogueCategory.MOVIE, "Arrival", 1))
                .isInstanceOf(CatalogueProviderException.class)
                .hasMessageContaining("TMDB_READ_ACCESS_TOKEN");
    }
}
