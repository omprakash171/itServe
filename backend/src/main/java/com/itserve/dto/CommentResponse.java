package com.itserve.dto;

import com.itserve.entity.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        UserBrief author,
        String authorRole,
        String message,
        Instant createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getTicket().getId(), UserBrief.from(c.getAuthor()),
                c.getAuthor().getRole().name(), c.getMessage(), c.getCreatedAt());
    }
}
