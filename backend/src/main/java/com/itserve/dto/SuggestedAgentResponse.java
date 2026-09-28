package com.itserve.dto;

import java.util.List;

/**
 * Suggested agent for a ticket. Score is deterministic and explainable:
 * skill match on the ticket category is the primary key, current active workload the secondary.
 */
public record SuggestedAgentResponse(
        Long agentId,
        String fullName,
        List<String> skills,
        boolean skillMatch,
        long activeTickets,
        long breachedTickets
) {
}
