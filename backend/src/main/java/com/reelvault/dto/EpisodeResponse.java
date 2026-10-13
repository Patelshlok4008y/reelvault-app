package com.reelvault.dto;

public record EpisodeResponse(
        Long id,
        int seasonNumber,
        int episodeNumber,
        String title,
        Integer runtimeMinutes,
        boolean watched
) { }
