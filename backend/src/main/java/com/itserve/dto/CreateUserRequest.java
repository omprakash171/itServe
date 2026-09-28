package com.itserve.dto;

import com.itserve.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @Size(max = 100) String department,
        List<String> skills
) {
}
