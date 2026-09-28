package com.itserve.dto;

import com.itserve.entity.KnowledgeArticle;

import java.time.Instant;

public record ArticleResponse(
        Long id,
        String title,
        String category,
        String content,
        UserBrief author,
        int helpfulYes,
        int helpfulNo,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(KnowledgeArticle a) {
        return new ArticleResponse(a.getId(), a.getTitle(), a.getCategory(), a.getContent(),
                UserBrief.from(a.getAuthor()), a.getHelpfulYes(), a.getHelpfulNo(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
