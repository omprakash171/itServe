package com.itserve.dto;

import com.itserve.entity.Role;
import com.itserve.entity.User;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String department,
        List<String> skills,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        List<String> skills = user.getSkills() == null || user.getSkills().isBlank()
                ? List.of()
                : Arrays.stream(user.getSkills().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getDepartment(), skills, user.isActive(), user.getCreatedAt());
    }
}
