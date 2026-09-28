package com.itserve.dto;

import java.util.Arrays;
import java.util.List;

public record AgentSummaryResponse(Long id, String fullName, List<String> skills) {
    public static AgentSummaryResponse from(com.itserve.entity.User user) {
        List<String> skillList = user.getSkills() == null || user.getSkills().isBlank()
                ? List.of()
                : Arrays.stream(user.getSkills().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new AgentSummaryResponse(user.getId(), user.getFullName(), skillList);
    }
}
