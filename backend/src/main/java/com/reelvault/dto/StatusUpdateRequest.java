package com.reelvault.dto;

import com.reelvault.domain.WatchStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull WatchStatus watchStatus) { }
