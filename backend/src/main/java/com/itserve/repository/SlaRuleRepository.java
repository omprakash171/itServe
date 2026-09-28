package com.itserve.repository;

import com.itserve.entity.Priority;
import com.itserve.entity.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaRuleRepository extends JpaRepository<SlaRule, Long> {

    Optional<SlaRule> findByPriority(Priority priority);
}
