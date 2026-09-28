package com.itserve.service;

import com.itserve.entity.Priority;
import com.itserve.entity.SlaRule;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.SlaRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SlaService {

    private final SlaRuleRepository slaRuleRepository;

    public SlaService(SlaRuleRepository slaRuleRepository) {
        this.slaRuleRepository = slaRuleRepository;
    }

    @Transactional(readOnly = true)
    public Instant computeDeadline(Priority priority, Instant createdAt) {
        SlaRule rule = slaRuleRepository.findByPriority(priority)
                .orElseThrow(() -> new ResourceNotFoundException("No SLA rule for priority " + priority));
        return createdAt.plus(rule.getResolutionHours(), ChronoUnit.HOURS);
    }

    @Transactional(readOnly = true)
    public java.util.List<SlaRule> allRules() {
        return slaRuleRepository.findAll();
    }

    @Transactional
    public SlaRule updateRule(Long ruleId, int resolutionHours) {
        if (resolutionHours <= 0 || resolutionHours > 168) {
            throw new IllegalArgumentException("resolutionHours must be between 1 and 168");
        }
        SlaRule rule = slaRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA rule not found"));
        rule.setResolutionHours(resolutionHours);
        return rule;
    }
}
