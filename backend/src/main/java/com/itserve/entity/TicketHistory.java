package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ticket_history", indexes = {
        @Index(name = "idx_history_ticket", columnList = "ticket_id")
})
@Getter
@Setter
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_history_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_id", foreignKey = @ForeignKey(name = "fk_history_performed_by"))
    private User performedBy;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TicketHistory() {
    }

    public TicketHistory(Ticket ticket, User performedBy, String action, String detail) {
        this.ticket = ticket;
        this.performedBy = performedBy;
        this.action = action;
        this.detail = detail;
    }
}
