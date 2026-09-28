package com.itserve.repository;

import com.itserve.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByTicketId(Long ticketId);

    List<Rating> findByTicketIdIn(Collection<Long> ticketIds);

    boolean existsByTicketId(Long ticketId);

    List<Rating> findByTicketAssignedAgentId(Long agentId);

    @Query("select avg(r.stars) from Rating r")
    Double averageStars();
}
