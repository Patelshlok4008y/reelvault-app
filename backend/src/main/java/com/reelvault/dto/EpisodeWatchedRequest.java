package com.reelvault.dto;

import jakarta.validation.constraints.NotNull;

public record EpisodeWatchedRequest(@NotNull Boolean watched) { }
