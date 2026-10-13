package com.reelvault.dto;

import java.util.List;

public record CataloguePage(int page, int perPage, int totalResults, int totalPages, List<CatalogueTitle> results) { }
