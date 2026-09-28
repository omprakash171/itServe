package com.itserve.dto;

import com.itserve.entity.Priority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 5, max = 200) String subject,
        @Size(min = 10, max = 4000) String description,
        Long categoryId,
        Priority priority,
        @Size(max = 100) String location,
        @Pattern(regexp = "EMAIL|PHONE|CHAT", message = "preferredContact must be EMAIL, PHONE or CHAT")
        String preferredContact
) {
}
