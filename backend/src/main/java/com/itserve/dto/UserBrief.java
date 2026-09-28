package com.itserve.dto;

public record UserBrief(Long id, String fullName, String email) {
    public static UserBrief from(com.itserve.entity.User user) {
        return user == null ? null : new UserBrief(user.getId(), user.getFullName(), user.getEmail());
    }
}
