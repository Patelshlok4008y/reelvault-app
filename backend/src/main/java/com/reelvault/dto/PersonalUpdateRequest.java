package com.reelvault.dto;

import com.reelvault.domain.WatchStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PersonalUpdateRequest(
        WatchStatus watchStatus,
        @Min(1) @Max(10) Integer personalRating,
        Boolean clearRating,
        @Size(max = 4000) String review,
        Boolean favourite,
        Boolean watchlist
) { }
