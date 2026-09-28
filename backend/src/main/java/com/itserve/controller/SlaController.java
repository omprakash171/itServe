package com.itserve.controller;

import com.itserve.dto.SlaRuleResponse;
import com.itserve.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla-rules")
@Tag(name = "SLA", description = "SLA target configuration per priority")
public class SlaController {

    public record UpdateSlaRuleRequest(@Min(1) @Max(168) int resolutionHours) {
    }

    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping
    @Operation(summary = "List SLA rules")
    public List<SlaRuleResponse> list() {
        return slaService.allRules().stream().map(SlaRuleResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an SLA target (ADMIN)")
    public SlaRuleResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSlaRuleRequest request) {
        return SlaRuleResponse.from(slaService.updateRule(id, request.resolutionHours()));
    }
}
