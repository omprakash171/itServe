package com.itserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank @Size(min = 1, max = 2000) String message
) {
}
