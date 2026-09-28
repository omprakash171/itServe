package com.itserve.dto;

import java.util.List;

public record AgentWorkloadResponse(
        Long agentId,
        String fullName,
        String email,
        List<String> skills,
        long assigned,
        long open,
        long inProgress,
        long resolved,
        long breached
) {
}
