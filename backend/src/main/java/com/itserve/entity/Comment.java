package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_ticket", columnList = "ticket_id")
})
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_comments_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(name = "fk_comments_author"))
    private User author;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
