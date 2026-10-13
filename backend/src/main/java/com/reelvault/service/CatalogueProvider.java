package com.reelvault.service;

import com.reelvault.domain.CatalogueCategory;
import com.reelvault.dto.CataloguePage;
import com.reelvault.dto.CatalogueTitle;

public interface CatalogueProvider {
    String provider();
    CataloguePage search(CatalogueCategory category, String query, int page);
    CatalogueTitle details(CatalogueCategory category, String externalId);
}
