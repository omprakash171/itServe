package com.itserve.controller;

import com.itserve.dto.*;
import com.itserve.entity.Attachment;
import com.itserve.entity.Priority;
import com.itserve.entity.TicketStatus;
import com.itserve.security.UserPrincipal;
import com.itserve.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Ticket CRUD, workflow, assignment and attachments")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "Search and filter tickets (paginated). Employees only ever see their own.")
    public PageResponse<TicketResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String sla,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.list(search, status, priority, categoryId, assignedTo, createdBy, sla,
                createdFrom, createdTo, page, size, sortBy, sortDir, principal.getUser());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ticket details with comments, activity history and attachments")
    public TicketDetailResponse detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.detail(id, principal.getUser());
    }

    @PostMapping
    @Operation(summary = "Raise a new ticket")
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.create(request, principal.getUser());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a ticket (creator while OPEN, or admin)")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.update(id, request, principal.getUser());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move a ticket through the validated status workflow")
    public TicketResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.updateStatus(id, request, principal.getUser());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a ticket to a support agent (ADMIN)")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.assign(id, request, principal.getUser());
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change priority and recalculate the SLA deadline (ADMIN)")
    public TicketResponse updatePriority(@PathVariable Long id, @Valid @RequestBody PriorityUpdateRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.updatePriority(id, request, principal.getUser());
    }

    @PatchMapping("/{id}/escalate")
    @Operation(summary = "Escalate a ticket")
    public TicketResponse escalate(@PathVariable Long id, @RequestBody(required = false) StatusUpdateRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.escalate(id, request == null ? new StatusUpdateRequest(null, null) : request,
                principal.getUser());
    }

    @GetMapping("/{id}/suggested-agents")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    @Operation(summary = "Rank agents for this ticket by skill match and current workload")
    public List<SuggestedAgentResponse> suggestedAgents(@PathVariable Long id) {
        return ticketService.suggestedAgents(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a ticket and its children (ADMIN)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        ticketService.delete(id, principal.getUser());
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Upload an attachment to a ticket")
    public AttachmentResponse uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ticketService.uploadAttachment(id, file, principal.getUser());
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        Attachment attachment = ticketService.getAttachment(attachmentId, principal.getUser());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        attachment.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : attachment.getContentType()))
                .body(new ByteArrayResource(attachment.getData()));
    }
}
