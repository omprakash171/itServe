package com.itserve.dto;

import com.itserve.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull TicketStatus status,
        @Size(max = 2000) String note
) {
}
