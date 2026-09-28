package com.itserve.service;

import com.itserve.dto.CommentRequest;
import com.itserve.dto.CommentResponse;
import com.itserve.entity.*;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.TicketAlreadyClosedException;
import com.itserve.repository.CommentRepository;
import com.itserve.repository.TicketHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, TicketHistoryRepository historyRepository,
                          TicketService ticketService, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForTicket(Long ticketId, User currentUser) {
        ticketService.findAccessible(ticketId, currentUser);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse add(Long ticketId, CommentRequest request, User currentUser) {
        Ticket ticket = ticketService.findAccessible(ticketId, currentUser);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketAlreadyClosedException("Cannot comment on a closed ticket");
        }
        boolean isParticipant = currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.SUPPORT_AGENT
                || ticket.getCreatedBy().getId().equals(currentUser.getId());
        if (!isParticipant) {
            throw new ForbiddenActionException("You cannot comment on this ticket");
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(currentUser);
        comment.setMessage(request.message().trim());
        commentRepository.save(comment);

        if (ticket.getFirstResponseAt() == null
                && (currentUser.getRole() == Role.SUPPORT_AGENT || currentUser.getRole() == Role.ADMIN)
                && ticket.getStatus() != TicketStatus.OPEN) {
            ticket.setFirstResponseAt(Instant.now());
        }

        historyRepository.save(new TicketHistory(ticket, currentUser, "COMMENT_ADDED",
                "Comment added by " + currentUser.getFullName()));

        notifyOthers(ticket, currentUser, request.message().trim());
        return CommentResponse.from(comment);
    }

    private void notifyOthers(Ticket ticket, User author, String message) {
        String preview = message.length() > 80 ? message.substring(0, 80) + "..." : message;
        String text = author.getFullName() + " commented on " + ticket.getTicketNumber() + ": \"" + preview + "\"";
        User creator = ticket.getCreatedBy();
        User agent = ticket.getAssignedAgent();
        if (!creator.getId().equals(author.getId())) {
            notificationService.notify(creator, ticket, NotificationType.COMMENT_ADDED, text);
        }
        if (agent != null && !agent.getId().equals(author.getId())) {
            notificationService.notify(agent, ticket, NotificationType.COMMENT_ADDED, text);
        }
    }
}
