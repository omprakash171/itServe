package com.itserve.dto;

import com.itserve.entity.Priority;
import com.itserve.entity.SlaRule;

public record SlaRuleResponse(Long id, Priority priority, int resolutionHours) {
    public static SlaRuleResponse from(SlaRule rule) {
        return new SlaRuleResponse(rule.getId(), rule.getPriority(), rule.getResolutionHours());
    }
}
