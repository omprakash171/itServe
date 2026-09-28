package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachments_ticket", columnList = "ticket_id")
})
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_attachments_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", foreignKey = @ForeignKey(name = "fk_attachments_uploaded_by"))
    private User uploadedBy;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    private long sizeBytes;

    @Lob
    @Column(name = "file_data")
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
