package com.itserve.repository;

import com.itserve.entity.Priority;
import com.itserve.entity.Ticket;
import com.itserve.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByCreatedByIdOrderByCreatedAtDesc(Long employeeId);

    List<Ticket> findByAssignedAgentIdAndStatusInOrderBySlaDeadlineAsc(Long agentId, List<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    long countByPriority(Priority priority);

    long countByCreatedById(Long userId);

    long countByCreatedByIdAndStatusIn(Long userId, List<TicketStatus> statuses);

    long countByAssignedAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);

    long countByStatusInAndSlaDeadlineBefore(List<TicketStatus> statuses, Instant instant);

    long countByAssignedAgentIdAndStatusInAndSlaDeadlineBefore(Long agentId, List<TicketStatus> statuses, Instant instant);

    List<Ticket> findByStatusInAndSlaDeadlineBefore(List<TicketStatus> statuses, Instant instant);

    List<Ticket> findByStatusInAndSlaDeadlineBetween(List<TicketStatus> statuses, Instant from, Instant to);

    List<Ticket> findByResolvedAtIsNotNull();

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countGroupByStatus();

    @Query("select t.priority, count(t) from Ticket t group by t.priority")
    List<Object[]> countGroupByPriority();

    @Query("select t.category.name, count(t) from Ticket t group by t.category.name")
    List<Object[]> countGroupByCategory();

    @Query("select t.category.name, count(t) from Ticket t where t.createdAt between :from and :to group by t.category.name")
    List<Object[]> countGroupByCategoryBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select t.assignedAgent.id, count(t) from Ticket t where t.status in :statuses group by t.assignedAgent.id")
    List<Object[]> countActiveGroupByAgent(@Param("statuses") List<TicketStatus> statuses);

    @Query("select max(t.ticketNumber) from Ticket t where t.ticketNumber like concat(:prefix, '%')")
    String findMaxTicketNumberWithPrefix(@Param("prefix") String prefix);

    List<Ticket> findTop10ByCreatedByIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findTop10ByOrderByCreatedAtDesc();
}
