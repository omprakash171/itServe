package com.itserve.dto;

public record CategoryResponse(Long id, String name, String description) {
    public static CategoryResponse from(com.itserve.entity.Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
