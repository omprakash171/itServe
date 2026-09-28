package com.itserve.dto;

import com.itserve.entity.TicketHistory;

import java.time.Instant;

public record HistoryResponse(
        Long id,
        String action,
        String detail,
        UserBrief performedBy,
        Instant createdAt
) {
    public static HistoryResponse from(TicketHistory h) {
        return new HistoryResponse(h.getId(), h.getAction(), h.getDetail(),
                UserBrief.from(h.getPerformedBy()), h.getCreatedAt());
    }
}
