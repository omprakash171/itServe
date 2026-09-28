package com.itserve.service;

import com.itserve.entity.Priority;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {

    static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_FOR_USER, TicketStatus.ESCALATED, TicketStatus.REOPENED);

    private static final long AT_RISK_MINUTES = 60;

    private TicketSpecifications() {
    }

    public static Specification<Ticket> search(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("ticketNumber")), like),
                    cb.like(cb.lower(root.get("subject")), like),
                    cb.like(cb.lower(root.join("createdBy").get("fullName")), like),
                    cb.like(cb.lower(root.join("assignedAgent", jakarta.persistence.criteria.JoinType.LEFT).get("fullName")), like));
        };
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(Priority priority) {
        return (root, query, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> inCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Ticket> createdBy(Long userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("createdBy").get("id"), userId);
    }

    public static Specification<Ticket> assignedTo(Long agentId) {
        return (root, query, cb) -> agentId == null ? cb.conjunction()
                : cb.equal(root.get("assignedAgent").get("id"), agentId);
    }

    public static Specification<Ticket> unassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignedAgent"));
    }

    public static Specification<Ticket> slaBreached() {
        return (root, query, cb) -> {
            Instant now = Instant.now();
            List<Predicate> or = new ArrayList<>();
            or.add(cb.and(root.get("status").in(ACTIVE_STATUSES), cb.lessThan(root.get("slaDeadline"), now)));
            or.add(cb.and(cb.isNotNull(root.get("resolvedAt")), cb.greaterThan(root.get("resolvedAt"), root.get("slaDeadline"))));
            return cb.or(or.toArray(new Predicate[0]));
        };
    }

    public static Specification<Ticket> slaAtRisk() {
        return (root, query, cb) -> {
            Instant now = Instant.now();
            Instant horizon = now.plus(AT_RISK_MINUTES, ChronoUnit.MINUTES);
            return cb.and(
                    root.get("status").in(ACTIVE_STATUSES),
                    cb.greaterThanOrEqualTo(root.get("slaDeadline"), now),
                    cb.lessThanOrEqualTo(root.get("slaDeadline"), horizon));
        };
    }

    public static Specification<Ticket> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            return from != null ? cb.greaterThanOrEqualTo(root.get("createdAt"), from)
                    : cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}
