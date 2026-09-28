package com.itserve.dto;

import com.itserve.entity.Rating;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long ticketId,
        String ticketNumber,
        int stars,
        String feedback,
        UserBrief createdBy,
        String agentName,
        Instant createdAt
) {
    public static RatingResponse from(Rating r) {
        return new RatingResponse(r.getId(), r.getTicket().getId(), r.getTicket().getTicketNumber(),
                r.getStars(), r.getFeedback(), UserBrief.from(r.getCreatedBy()),
                r.getTicket().getAssignedAgent() == null ? null : r.getTicket().getAssignedAgent().getFullName(),
                r.getCreatedAt());
    }
}
