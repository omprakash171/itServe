package com.itserve.dto;

import com.itserve.entity.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String ticketNumber,
        String subject,
        String description,
        TicketStatus status,
        Priority priority,
        CategoryResponse category,
        UserBrief createdBy,
        UserBrief assignedAgent,
        String location,
        String preferredContact,
        Instant slaDeadline,
        String slaStatus,
        Long slaRemainingMinutes,
        Instant firstResponseAt,
        Instant resolvedAt,
        String resolutionNote,
        boolean rated,
        Integer ratingStars,
        List<AttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {

    /** SLA_AT_RISK_THRESHOLD: less than 25% of a typical workday target left counts as "at risk". */
    private static final long AT_RISK_MINUTES = 60;

    public static TicketResponse from(Ticket t, boolean rated, Integer ratingStars, List<AttachmentResponse> attachments) {
        String slaStatus = "NONE";
        Long remaining = null;
        if (t.getSlaDeadline() != null) {
            boolean finished = t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED;
            Instant effectiveNow = finished && t.getResolvedAt() != null ? t.getResolvedAt() : Instant.now();
            long minutesLeft = Duration.between(effectiveNow, t.getSlaDeadline()).toMinutes();
            remaining = minutesLeft;
            if (minutesLeft < 0) {
                slaStatus = "BREACHED";
            } else if (finished) {
                slaStatus = "MET";
            } else if (minutesLeft <= AT_RISK_MINUTES) {
                slaStatus = "AT_RISK";
            } else {
                slaStatus = "OK";
            }
        }
        return new TicketResponse(
                t.getId(), t.getTicketNumber(), t.getSubject(), t.getDescription(), t.getStatus(), t.getPriority(),
                CategoryResponse.from(t.getCategory()), UserBrief.from(t.getCreatedBy()), UserBrief.from(t.getAssignedAgent()),
                t.getLocation(), t.getPreferredContact(), t.getSlaDeadline(), slaStatus, remaining,
                t.getFirstResponseAt(), t.getResolvedAt(), t.getResolutionNote(),
                rated, ratingStars, attachments, t.getCreatedAt(), t.getUpdatedAt());
    }
}
