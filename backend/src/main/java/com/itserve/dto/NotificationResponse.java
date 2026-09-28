package com.itserve.dto;

import com.itserve.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        Long ticketId,
        String ticketNumber,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getMessage(),
                n.getTicket() == null ? null : n.getTicket().getId(),
                n.getTicket() == null ? null : n.getTicket().getTicketNumber(),
                n.isRead(), n.getCreatedAt());
    }
}
