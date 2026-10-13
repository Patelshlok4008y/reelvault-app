package com.reelvault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EpisodeRequest(
        @NotNull @Min(1) Integer seasonNumber,
        @NotNull @Min(1) Integer episodeNumber,
        @NotBlank @Size(max = 180) String title,
        @Min(1) @Max(1000) Integer runtimeMinutes
) { }
