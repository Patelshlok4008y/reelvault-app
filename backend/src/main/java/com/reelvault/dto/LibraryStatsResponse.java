package com.reelvault.dto;

public record LibraryStatsResponse(
        long totalTitles,
        long movieCount,
        long seriesCount,
        long completedCount,
        long favouriteCount
) { }
