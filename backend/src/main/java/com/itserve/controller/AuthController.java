package com.itserve.controller;

import com.itserve.dto.LoginRequest;
import com.itserve.dto.LoginResponse;
import com.itserve.dto.RegisterRequest;
import com.itserve.dto.UserResponse;
import com.itserve.security.UserPrincipal;
import com.itserve.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, registration and current-user endpoints")
public class AuthController {

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email/password and receive a JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @Operation(summary = "Self-register as an EMPLOYEE")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }

    @PatchMapping("/password")
    @Operation(summary = "Change own password")
    public UserResponse changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(principal, request.currentPassword(), request.newPassword());
    }
}
