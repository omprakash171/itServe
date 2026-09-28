package com.itserve.controller;

import com.itserve.entity.Role;
import com.itserve.security.UserPrincipal;
import com.itserve.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Role-specific dashboard analytics computed from live data")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "System-wide KPIs, charts, agent workload and incident trends")
    public Map<String, Object> admin() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/agent")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    @Operation(summary = "Agent KPIs and today's tasks")
    public Map<String, Object> agent(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.agentDashboard(principal.getUser());
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Employee ticket summary and recent tickets")
    public Map<String, Object> employee(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.employeeDashboard(principal.getUser());
    }

    @GetMapping("/me")
    @Operation(summary = "Role-aware dashboard entry point")
    public Map<String, Object> me(@AuthenticationPrincipal UserPrincipal principal) {
        Role role = principal.getRole();
        return switch (role) {
            case ADMIN -> dashboardService.adminDashboard();
            case SUPPORT_AGENT -> dashboardService.agentDashboard(principal.getUser());
            case EMPLOYEE -> dashboardService.employeeDashboard(principal.getUser());
        };
    }
}
