package com.itserve.controller;

import com.itserve.dto.RatingRequest;
import com.itserve.dto.RatingResponse;
import com.itserve.security.UserPrincipal;
import com.itserve.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Ratings", description = "Post-resolution support ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/api/tickets/{id}/rating")
    @Operation(summary = "Rate the support on a resolved ticket (ticket owner only)")
    public RatingResponse rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return ratingService.rate(id, request, principal.getUser());
    }

    @GetMapping("/api/ratings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    @Operation(summary = "List all ratings (ADMIN / SUPPORT_AGENT analytics)")
    public List<RatingResponse> listAll() {
        return ratingService.listAll();
    }

    @GetMapping("/api/tickets/{id}/rating")
    @Operation(summary = "Get the rating for a ticket")
    public RatingResponse forTicket(@PathVariable Long id) {
        return ratingService.forTicket(id);
    }
}
