package com.itserve.service;

import com.itserve.dto.RatingRequest;
import com.itserve.dto.RatingResponse;
import com.itserve.entity.Rating;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketStatus;
import com.itserve.entity.User;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.InvalidTicketStatusException;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.RatingRepository;
import com.itserve.repository.TicketHistoryRepository;
import com.itserve.entity.TicketHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TicketService ticketService;
    private final TicketHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public RatingService(RatingRepository ratingRepository, TicketService ticketService,
                         TicketHistoryRepository historyRepository, NotificationService notificationService) {
        this.ratingRepository = ratingRepository;
        this.ticketService = ticketService;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public RatingResponse rate(Long ticketId, RatingRequest request, User currentUser) {
        Ticket ticket = ticketService.findAccessible(ticketId, currentUser);
        if (!ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("Only the ticket owner can rate the support");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new InvalidTicketStatusException("Tickets can only be rated after they are resolved");
        }
        if (ratingRepository.existsByTicketId(ticketId)) {
            throw new InvalidTicketStatusException("This ticket has already been rated");
        }

        Rating rating = new Rating();
        rating.setTicket(ticket);
        rating.setCreatedBy(currentUser);
        rating.setStars(request.stars());
        rating.setFeedback(request.feedback());
        ratingRepository.save(rating);

        historyRepository.save(new TicketHistory(ticket, currentUser, "RATED",
                "Rated " + request.stars() + "/5 stars"));
        if (ticket.getAssignedAgent() != null) {
            notificationService.notify(ticket.getAssignedAgent(), ticket,
                    com.itserve.entity.NotificationType.STATUS_CHANGED,
                    "Ticket " + ticket.getTicketNumber() + " was rated " + request.stars() + "/5");
        }
        return RatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> listAll() {
        return ratingRepository.findAll().stream().map(RatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RatingResponse forTicket(Long ticketId) {
        Rating rating = ratingRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("No rating for this ticket"));
        return RatingResponse.from(rating);
    }
}
