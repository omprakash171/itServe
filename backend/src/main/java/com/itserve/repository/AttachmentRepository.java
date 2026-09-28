package com.itserve.repository;

import com.itserve.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
