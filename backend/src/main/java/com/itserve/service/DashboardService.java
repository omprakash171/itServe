package com.itserve.service;

import com.itserve.dto.AgentWorkloadResponse;
import com.itserve.dto.TicketResponse;
import com.itserve.entity.*;
import com.itserve.repository.RatingRepository;
import com.itserve.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_FOR_USER, TicketStatus.ESCALATED, TicketStatus.REOPENED);

    private final TicketRepository ticketRepository;
    private final RatingRepository ratingRepository;
    private final TicketService ticketService;

    public DashboardService(TicketRepository ticketRepository, RatingRepository ratingRepository,
                            TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ratingRepository = ratingRepository;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        List<Ticket> tickets = ticketRepository.findAll();
        Instant now = Instant.now();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status.name(), tickets.stream().filter(t -> t.getStatus() == status).count());
        }
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            byPriority.put(priority.name(), tickets.stream().filter(t -> t.getPriority() == priority).count());
        }
        Map<String, Long> byCategory = new LinkedHashMap<>();
        tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getCategory().getName(), LinkedHashMap::new, Collectors.counting()))
                .forEach(byCategory::put);

        long open = tickets.stream().filter(t -> ACTIVE_STATUSES.contains(t.getStatus())).count();
        long inProgress = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS || t.getStatus() == TicketStatus.WAITING_FOR_USER)
                .count();
        List<Ticket> resolved = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED)
                .toList();
        long breached = tickets.stream().filter(t -> isBreached(t, now)).count();
        long critical = tickets.stream().filter(t -> t.getPriority() == Priority.CRITICAL).count();

        long resolvedWithinSla = resolved.stream()
                .filter(t -> t.getResolvedAt() != null && t.getSlaDeadline() != null
                        && !t.getResolvedAt().isAfter(t.getSlaDeadline()))
                .count();
        double slaCompliancePct = resolved.isEmpty() ? 100.0
                : round1(resolvedWithinSla * 100.0 / resolved.size());
        double avgResolutionHours = resolved.stream()
                .filter(t -> t.getResolvedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes() / 60.0)
                .average().orElse(0);
        double avgFirstResponseHours = tickets.stream()
                .filter(t -> t.getFirstResponseAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getFirstResponseAt()).toMinutes() / 60.0)
                .average().orElse(0);
        Double avgRating = ratingRepository.averageStars();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalTickets", tickets.size());
        kpis.put("openTickets", open);
        kpis.put("inProgress", inProgress);
        kpis.put("resolved", resolved.size());
        kpis.put("slaBreached", breached);
        kpis.put("criticalTickets", critical);
        kpis.put("slaCompliancePct", slaCompliancePct);
        kpis.put("avgResolutionHours", round1(avgResolutionHours));
        kpis.put("avgFirstResponseHours", round1(avgFirstResponseHours));
        kpis.put("avgRating", avgRating == null ? null : Math.round(avgRating * 10) / 10.0);
        kpis.put("resolutionRatePct", tickets.isEmpty() ? 0.0 : round1(resolved.size() * 100.0 / tickets.size()));

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("kpis", kpis);
        dashboard.put("byStatus", toSeries(byStatus));
        dashboard.put("byPriority", toSeries(byPriority));
        dashboard.put("byCategory", toSeries(byCategory));
        dashboard.put("createdOverTime", dailySeries(tickets, Ticket::getCreatedAt, 14));
        dashboard.put("resolvedOverTime", dailySeries(tickets, Ticket::getResolvedAt, 14));

        List<AgentWorkloadResponse> workloads = ticketService.agentWorkloads();
        dashboard.put("agentWorkload", workloads);
        dashboard.put("incidentTrends", detectIncidentTrends(tickets, now));
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> agentDashboard(User agent) {
        Instant now = Instant.now();
        List<Ticket> mine = ticketRepository.findByAssignedAgentIdAndStatusInOrderBySlaDeadlineAsc(
                agent.getId(), ACTIVE_STATUSES);
        long resolvedCount = ticketRepository.countByAssignedAgentIdAndStatusIn(
                agent.getId(), List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
        long breached = mine.stream().filter(t -> isBreached(t, now)).count();
        long atRisk = mine.stream()
                .filter(t -> !isBreached(t, now) && t.getSlaDeadline() != null
                        && t.getSlaDeadline().isBefore(now.plusSeconds(3600)))
                .count();

        List<Map<String, Object>> todaysTasks = mine.stream()
                .sorted(Comparator.comparing(Ticket::getSlaDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .map(t -> {
                    TicketResponse response = TicketResponse.from(t, false, null, List.of());
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("id", t.getId());
                    task.put("ticketNumber", t.getTicketNumber());
                    task.put("subject", t.getSubject());
                    task.put("priority", t.getPriority().name());
                    task.put("status", t.getStatus().name());
                    task.put("slaStatus", response.slaStatus());
                    task.put("slaRemainingMinutes", response.slaRemainingMinutes());
                    return task;
                })
                .toList();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("kpis", Map.of(
                "myOpenTickets", mine.size(),
                "myInProgress", mine.stream()
                        .filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS || t.getStatus() == TicketStatus.WAITING_FOR_USER)
                        .count(),
                "myResolved", resolvedCount,
                "slaAtRisk", atRisk,
                "slaBreached", breached));
        dashboard.put("todaysTasks", todaysTasks);
        dashboard.put("agentName", agent.getFullName());
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> employeeDashboard(User employee) {
        List<Ticket> mine = ticketRepository.findByCreatedByIdOrderByCreatedAtDesc(employee.getId());

        long open = mine.stream().filter(t -> ACTIVE_STATUSES.contains(t.getStatus())).count();
        long waitingForUser = mine.stream().filter(t -> t.getStatus() == TicketStatus.WAITING_FOR_USER).count();
        long resolved = mine.stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED).count();
        long closed = mine.stream().filter(t -> t.getStatus() == TicketStatus.CLOSED).count();

        List<TicketResponse> recent = mine.stream().limit(8)
                .map(t -> TicketResponse.from(t, false, null, List.of()))
                .toList();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("kpis", Map.of(
                "myOpenTickets", open,
                "myPendingTickets", waitingForUser,
                "myResolvedTickets", resolved,
                "myClosedTickets", closed,
                "totalTickets", mine.size()));
        dashboard.put("recentTickets", recent);
        return dashboard;
    }

    private boolean isBreached(Ticket ticket, Instant now) {
        if (ticket.getSlaDeadline() == null) {
            return false;
        }
        if (ACTIVE_STATUSES.contains(ticket.getStatus())) {
            return now.isAfter(ticket.getSlaDeadline());
        }
        return ticket.getResolvedAt() != null && ticket.getResolvedAt().isAfter(ticket.getSlaDeadline());
    }

    /**
     * Rule-based incident trend detection: compares per-category ticket volume of the last
     * 7 days against the previous 7 days. >= 50% increase with at least 3 tickets is flagged.
     */
    private List<Map<String, Object>> detectIncidentTrends(List<Ticket> tickets, Instant now) {
        Instant weekAgo = now.minus(Duration.ofDays(7));
        Instant twoWeeksAgo = now.minus(Duration.ofDays(14));

        Map<String, Long> current = countByCategoryBetween(tickets, weekAgo, now);
        Map<String, Long> previous = countByCategoryBetween(tickets, twoWeeksAgo, weekAgo);

        List<Map<String, Object>> trends = new ArrayList<>();
        Set<String> categories = new LinkedHashSet<>();
        categories.addAll(current.keySet());
        categories.addAll(previous.keySet());
        for (String category : categories) {
            long curr = current.getOrDefault(category, 0L);
            long prev = previous.getOrDefault(category, 0L);
            if (curr < 3) {
                continue;
            }
            double increasePct = prev == 0 ? 100.0 : (curr - prev) * 100.0 / prev;
            if (increasePct >= 50.0) {
                Map<String, Object> trend = new LinkedHashMap<>();
                trend.put("category", category);
                trend.put("previousCount", prev);
                trend.put("currentCount", curr);
                trend.put("increasePct", Math.round(increasePct));
                trend.put("message", category + "-related tickets increased " + Math.round(increasePct)
                        + "% this week (" + prev + " -> " + curr + "). Possible action: investigate "
                        + category + " infrastructure.");
                trends.add(trend);
            }
        }
        trends.sort((a, b) -> Long.compare((long) b.get("currentCount"), (long) a.get("currentCount")));
        return trends;
    }

    private Map<String, Long> countByCategoryBetween(List<Ticket> tickets, Instant from, Instant to) {
        return tickets.stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(from) && t.getCreatedAt().isBefore(to))
                .collect(Collectors.groupingBy(t -> t.getCategory().getName(), Collectors.counting()));
    }

    private List<Map<String, Object>> dailySeries(List<Ticket> tickets,
                                                  java.util.function.Function<Ticket, Instant> timeExtractor,
                                                  int days) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Map<LocalDate, Long> counts = tickets.stream()
                .map(timeExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(instant -> instant.atZone(zone).toLocalDate(), Collectors.counting()));

        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            series.add(Map.of("label", day.toString(), "value", counts.getOrDefault(day, 0L)));
        }
        return series;
    }

    private List<Map<String, Object>> toSeries(Map<String, Long> counts) {
        List<Map<String, Object>> series = new ArrayList<>();
        counts.forEach((label, value) -> series.add(Map.of("label", label, "value", value)));
        return series;
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
