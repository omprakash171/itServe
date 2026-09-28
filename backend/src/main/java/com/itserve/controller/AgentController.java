package com.itserve.controller;

import com.itserve.dto.AgentSummaryResponse;
import com.itserve.dto.AgentWorkloadResponse;
import com.itserve.service.TicketService;
import com.itserve.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agents", description = "Support agent directory and workload (ADMIN and SUPPORT_AGENT)")
public class AgentController {

    private final TicketService ticketService;
    private final UserService userService;

    public AgentController(TicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List active support agents (id, name, skills)")
    public List<AgentSummaryResponse> agents() {
        return userService.listActiveAgents();
    }

    @GetMapping("/workload")
    @Operation(summary = "Per-agent ticket workload summary")
    public List<AgentWorkloadResponse> workload() {
        return ticketService.agentWorkloads();
    }
}
