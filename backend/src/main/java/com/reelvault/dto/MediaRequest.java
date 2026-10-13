package com.reelvault.dto;

import com.reelvault.domain.MediaType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MediaRequest(
        @NotNull MediaType mediaType,
        @NotBlank @Size(max = 180) String title,
        @NotNull @Min(1888) @Max(2100) Integer releaseYear,
        @Size(max = 4000) String description,
        @Size(max = 500) String genres,
        @Size(max = 1000) String posterUrl,
        @Size(max = 1000) String backdropUrl,
        @Min(1) @Max(1000) Integer runtimeMinutes
) { }
