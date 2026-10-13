package com.reelvault.dto;

import com.reelvault.domain.CatalogueCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportRequest(@NotBlank String provider, @NotBlank String externalId, @NotNull CatalogueCategory category) { }
