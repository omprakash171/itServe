package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_number", columnList = "ticket_number", unique = true),
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_priority", columnList = "priority"),
        @Index(name = "idx_tickets_created_at", columnList = "created_at"),
        @Index(name = "idx_tickets_assigned_agent", columnList = "assigned_agent_id"),
        @Index(name = "idx_tickets_category", columnList = "category_id"),
        @Index(name = "idx_tickets_created_by", columnList = "created_by_id")
})
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 20)
    private String ticketNumber;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_tickets_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", foreignKey = @ForeignKey(name = "fk_tickets_created_by"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id", foreignKey = @ForeignKey(name = "fk_tickets_assigned_agent"))
    private User assignedAgent;

    private String location;

    @Column(name = "preferred_contact", length = 20)
    private String preferredContact;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    private boolean slaBreachedNotified;
    private boolean slaAtRiskNotified;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = 2000)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
