package com.itserve.service;

import com.itserve.entity.NotificationType;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketHistory;
import com.itserve.entity.TicketStatus;
import com.itserve.repository.TicketHistoryRepository;
import com.itserve.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodically scans active tickets and raises SLA_AT_RISK / SLA_BREACHED notifications.
 * Tickets carry notified-flags so each state is only notified once.
 */
@Component
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);
    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_FOR_USER, TicketStatus.ESCALATED, TicketStatus.REOPENED);
    private static final long AT_RISK_WINDOW_MINUTES = 60;

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public SlaMonitor(TicketRepository ticketRepository, TicketHistoryRepository historyRepository,
                      NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 900_000)
    @Transactional
    public void checkSlas() {
        Instant now = Instant.now();

        for (Ticket ticket : ticketRepository.findByStatusInAndSlaDeadlineBefore(ACTIVE_STATUSES, now)) {
            if (ticket.isSlaBreachedNotified()) {
                continue;
            }
            ticket.setSlaBreachedNotified(true);
            historyRepository.save(new TicketHistory(ticket, ticket.getCreatedBy(), "SLA_BREACHED",
                    "SLA deadline passed without resolution"));
            notifyStakeholders(ticket, NotificationType.SLA_BREACHED,
                    "Ticket " + ticket.getTicketNumber() + " SLA breached");
            log.info("SLA breached for ticket {}", ticket.getTicketNumber());
        }

        Instant atRiskHorizon = now.plus(AT_RISK_WINDOW_MINUTES, ChronoUnit.MINUTES);
        for (Ticket ticket : ticketRepository.findByStatusInAndSlaDeadlineBetween(ACTIVE_STATUSES, now, atRiskHorizon)) {
            if (ticket.isSlaAtRiskNotified() || ticket.isSlaBreachedNotified()) {
                continue;
            }
            ticket.setSlaAtRiskNotified(true);
            notifyStakeholders(ticket, NotificationType.SLA_AT_RISK,
                    "Ticket " + ticket.getTicketNumber() + " SLA expires in under " + AT_RISK_WINDOW_MINUTES + " minutes");
        }
    }

    private void notifyStakeholders(Ticket ticket, NotificationType type, String message) {
        if (ticket.getAssignedAgent() != null) {
            notificationService.notify(ticket.getAssignedAgent(), ticket, type, message);
        }
        notificationService.notify(ticket.getCreatedBy(), ticket, type, message);
    }
}
