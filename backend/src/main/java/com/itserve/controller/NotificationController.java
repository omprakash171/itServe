package com.itserve.controller;

import com.itserve.dto.NotificationResponse;
import com.itserve.dto.PageResponse;
import com.itserve.security.UserPrincipal;
import com.itserve.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the current user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List my notifications (paginated, newest first)")
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return notificationService.listForUser(principal.getId(), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count of unread notifications (for the navbar bell)")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getId());
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public void markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
    }
}
