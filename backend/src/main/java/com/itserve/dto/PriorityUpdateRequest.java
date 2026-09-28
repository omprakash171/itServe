package com.itserve.dto;

import com.itserve.entity.Priority;
import jakarta.validation.constraints.NotNull;

public record PriorityUpdateRequest(@NotNull Priority priority) {
}
