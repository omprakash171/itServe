package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "knowledge_articles", indexes = {
        @Index(name = "idx_articles_category", columnList = "category")
})
@Getter
@Setter
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 8000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(name = "fk_articles_author"))
    private User author;

    @Column(name = "helpful_yes")
    private int helpfulYes;

    @Column(name = "helpful_no")
    private int helpfulNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
