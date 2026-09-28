package com.itserve.dto;

import com.itserve.entity.Priority;
import jakarta.validation.constraints.*;

public record CreateTicketRequest(
        @NotBlank @Size(min = 5, max = 200) String subject,
        @NotBlank @Size(min = 10, max = 4000) String description,
        @NotNull Long categoryId,
        @NotNull Priority priority,
        @Size(max = 100) String location,
        @Pattern(regexp = "EMAIL|PHONE|CHAT", message = "preferredContact must be EMAIL, PHONE or CHAT")
        String preferredContact
) {
}
