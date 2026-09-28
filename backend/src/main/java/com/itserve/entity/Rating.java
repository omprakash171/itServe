package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ratings", indexes = {
        @Index(name = "idx_ratings_ticket", columnList = "ticket_id", unique = true)
})
@Getter
@Setter
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", unique = true, foreignKey = @ForeignKey(name = "fk_ratings_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", foreignKey = @ForeignKey(name = "fk_ratings_created_by"))
    private User createdBy;

    @Column(nullable = false)
    private int stars;

    @Column(length = 1000)
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
