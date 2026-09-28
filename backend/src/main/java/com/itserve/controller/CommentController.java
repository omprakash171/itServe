package com.itserve.controller;

import com.itserve.dto.CommentRequest;
import com.itserve.dto.CommentResponse;
import com.itserve.security.UserPrincipal;
import com.itserve.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@Tag(name = "Comments", description = "Ticket conversation")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "List comments on a ticket")
    public List<CommentResponse> list(@PathVariable Long ticketId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.listForTicket(ticketId, principal.getUser());
    }

    @PostMapping
    @Operation(summary = "Add a comment to a ticket")
    public CommentResponse add(@PathVariable Long ticketId, @Valid @RequestBody CommentRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.add(ticketId, request, principal.getUser());
    }
}
