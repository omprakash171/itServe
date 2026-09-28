package com.itserve.service;

import com.itserve.entity.Role;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketStatus;
import com.itserve.entity.User;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.InvalidTicketStatusException;
import com.itserve.exception.TicketAlreadyClosedException;

import java.util.Map;
import java.util.Set;

/**
 * Central place for the ticket status state machine and who may trigger which transition.
 *
 * OPEN -> ASSIGNED -> IN_PROGRESS -> WAITING_FOR_USER -> RESOLVED -> CLOSED
 * RESOLVED -> REOPENED -> IN_PROGRESS
 * OPEN / ASSIGNED / IN_PROGRESS -> ESCALATED -> ASSIGNED / IN_PROGRESS
 */
public final class TicketWorkflow {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ESCALATED),
            TicketStatus.ASSIGNED, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.ESCALATED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.WAITING_FOR_USER, TicketStatus.RESOLVED, TicketStatus.ESCALATED),
            TicketStatus.WAITING_FOR_USER, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            TicketStatus.ESCALATED, Set.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS),
            TicketStatus.REOPENED, Set.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED, TicketStatus.REOPENED),
            TicketStatus.CLOSED, Set.of()
    );

    private TicketWorkflow() {
    }

    public static Set<TicketStatus> allowedTransitions(TicketStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }

    public static void validate(Role actorRole, User actor, Ticket ticket, TicketStatus target) {
        TicketStatus current = ticket.getStatus();
        if (current == TicketStatus.CLOSED) {
            throw new TicketAlreadyClosedException(
                    "Ticket " + ticket.getTicketNumber() + " is closed and cannot be modified");
        }
        if (!allowedTransitions(current).contains(target)) {
            throw new InvalidTicketStatusException(
                    "Cannot move ticket from " + current + " to " + target
                            + ". Allowed: " + allowedTransitions(current));
        }
        switch (actorRole) {
            case EMPLOYEE -> {
                if (!ticket.getCreatedBy().getId().equals(actor.getId())) {
                    throw new ForbiddenActionException("You can only act on your own tickets");
                }
                if (target != TicketStatus.CLOSED && target != TicketStatus.REOPENED) {
                    throw new ForbiddenActionException(
                            "Employees can only close or reopen resolved tickets");
                }
            }
            case SUPPORT_AGENT -> {
                if (target == TicketStatus.CLOSED || target == TicketStatus.REOPENED) {
                    throw new ForbiddenActionException(
                            "Only the ticket owner or an admin can close or reopen tickets");
                }
                boolean isAssignee = ticket.getAssignedAgent() != null
                        && ticket.getAssignedAgent().getId().equals(actor.getId());
                if (!isAssignee && current != TicketStatus.OPEN && current != TicketStatus.ESCALATED) {
                    throw new ForbiddenActionException("Only the assigned agent can update this ticket");
                }
            }
            case ADMIN -> {
                // Admins may override any valid transition.
            }
        }
    }
}
