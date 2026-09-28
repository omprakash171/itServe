package com.itserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleRequest(
        @NotBlank @Size(min = 5, max = 200) String title,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(min = 20, max = 8000) String content
) {
}
