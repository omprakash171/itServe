package com.itserve.controller;

import com.itserve.dto.CategoryResponse;
import com.itserve.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Ticket categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "List all ticket categories")
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }
}
