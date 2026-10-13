package com.reelvault.dto;

import java.util.List;

public record DashboardResponse(
        MediaResponse featured,
        LibraryStatsResponse stats,
        List<MediaResponse> continueWatching,
        List<MediaResponse> recentlyAdded
) { }
