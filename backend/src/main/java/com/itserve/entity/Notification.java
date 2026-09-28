package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notifications_read", columnList = "is_read")
})
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", foreignKey = @ForeignKey(name = "fk_notifications_recipient"))
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_notifications_ticket"))
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {
    }

    public Notification(User recipient, Ticket ticket, NotificationType type, String message) {
        this.recipient = recipient;
        this.ticket = ticket;
        this.type = type;
        this.message = message;
    }
}
