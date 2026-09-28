package com.itserve.service;

import com.itserve.entity.Role;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketStatus;
import com.itserve.entity.User;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.InvalidTicketStatusException;
import com.itserve.exception.TicketAlreadyClosedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketWorkflowTest {

    private User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Ticket ticket(TicketStatus status, User creator, User agent) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("INC-2026-000001");
        ticket.setStatus(status);
        ticket.setCreatedBy(creator);
        ticket.setAssignedAgent(agent);
        return ticket;
    }

    @Test
    void allowsTheHappyPathTransitions() {
        TicketStatus[] happyPath = {
                TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING_FOR_USER, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED,
                TicketStatus.CLOSED};
        User admin = user(1, Role.ADMIN);
        Ticket ticket = ticket(happyPath[0], admin, null);
        for (int i = 1; i < happyPath.length; i++) {
            TicketStatus target = happyPath[i];
            assertDoesNotThrow(() -> TicketWorkflow.validate(Role.ADMIN, admin, ticket, target));
            ticket.setStatus(target);
        }
    }

    @Test
    void rejectsSkippingFromOpenToResolved() {
        User admin = user(1, Role.ADMIN);
        Ticket ticket = ticket(TicketStatus.OPEN, admin, null);
        assertThrows(InvalidTicketStatusException.class,
                () -> TicketWorkflow.validate(Role.ADMIN, admin, ticket, TicketStatus.RESOLVED));
    }

    @Test
    void rejectsAnyTransitionFromClosed() {
        User admin = user(1, Role.ADMIN);
        Ticket ticket = ticket(TicketStatus.CLOSED, admin, null);
        assertThrows(TicketAlreadyClosedException.class,
                () -> TicketWorkflow.validate(Role.ADMIN, admin, ticket, TicketStatus.REOPENED));
    }

    @Test
    void employeeCanOnlyCloseOrReopenOwnResolvedTicket() {
        User employee = user(2, Role.EMPLOYEE);
        User agent = user(3, Role.SUPPORT_AGENT);

        Ticket resolved = ticket(TicketStatus.RESOLVED, employee, agent);
        assertDoesNotThrow(() -> TicketWorkflow.validate(Role.EMPLOYEE, employee, resolved, TicketStatus.CLOSED));
        assertDoesNotThrow(() -> TicketWorkflow.validate(Role.EMPLOYEE, employee, resolved, TicketStatus.REOPENED));

        Ticket inProgress = ticket(TicketStatus.IN_PROGRESS, employee, agent);
        assertThrows(ForbiddenActionException.class,
                () -> TicketWorkflow.validate(Role.EMPLOYEE, employee, inProgress, TicketStatus.RESOLVED));

        Ticket someElsesTicket = ticket(TicketStatus.RESOLVED, user(9, Role.EMPLOYEE), agent);
        assertThrows(ForbiddenActionException.class,
                () -> TicketWorkflow.validate(Role.EMPLOYEE, employee, someElsesTicket, TicketStatus.CLOSED));
    }

    @Test
    void agentCannotCloseOrReopenAndMustBeAssignee() {
        User agent = user(3, Role.SUPPORT_AGENT);
        User employee = user(2, Role.EMPLOYEE);

        Ticket resolved = ticket(TicketStatus.RESOLVED, employee, agent);
        assertThrows(ForbiddenActionException.class,
                () -> TicketWorkflow.validate(Role.SUPPORT_AGENT, agent, resolved, TicketStatus.CLOSED));

        Ticket assigned = ticket(TicketStatus.ASSIGNED, employee, agent);
        assertDoesNotThrow(() -> TicketWorkflow.validate(Role.SUPPORT_AGENT, agent, assigned, TicketStatus.IN_PROGRESS));

        Ticket assignedToSomeoneElse = ticket(TicketStatus.ASSIGNED, employee, user(4, Role.SUPPORT_AGENT));
        assertThrows(ForbiddenActionException.class,
                () -> TicketWorkflow.validate(Role.SUPPORT_AGENT, agent, assignedToSomeoneElse, TicketStatus.IN_PROGRESS));
    }

    @Test
    void escalationIsAllowedFromOpenAssignedAndInProgress() {
        assertTrue(TicketWorkflow.allowedTransitions(TicketStatus.OPEN).contains(TicketStatus.ESCALATED));
        assertTrue(TicketWorkflow.allowedTransitions(TicketStatus.ASSIGNED).contains(TicketStatus.ESCALATED));
        assertTrue(TicketWorkflow.allowedTransitions(TicketStatus.IN_PROGRESS).contains(TicketStatus.ESCALATED));
        assertFalse(TicketWorkflow.allowedTransitions(TicketStatus.RESOLVED).contains(TicketStatus.ESCALATED));
    }

    @Test
    void reopenedTicketCanGoBackToInProgress() {
        assertTrue(TicketWorkflow.allowedTransitions(TicketStatus.REOPENED).contains(TicketStatus.IN_PROGRESS));
        assertTrue(TicketWorkflow.allowedTransitions(TicketStatus.RESOLVED).contains(TicketStatus.REOPENED));
    }
}
