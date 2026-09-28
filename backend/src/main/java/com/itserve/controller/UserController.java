package com.itserve.controller;

import com.itserve.dto.CreateUserRequest;
import com.itserve.dto.UpdateUserRequest;
import com.itserve.dto.UserResponse;
import com.itserve.entity.Role;
import com.itserve.security.UserPrincipal;
import com.itserve.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management (ADMIN only)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users, optionally filtered by role")
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.list(role);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a user with any role")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's profile/role fields")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a user (soft delete)")
    public void deactivate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        userService.deactivate(id, principal.getUser());
    }
}
