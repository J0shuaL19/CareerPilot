package com.careerpilot.dto;

import com.careerpilot.model.JobActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record JobActivityCalendarImportItemRequest(
        @NotNull(message = "Job is required")
        @Positive(message = "Job must be positive")
        Long jobId,

        @NotNull(message = "Activity type is required")
        JobActivityType type,

        @NotBlank(message = "Activity title is required")
        @Size(max = 255, message = "Activity title must be 255 characters or fewer")
        String title,

        @Size(max = 5000, message = "Activity details must be 5000 characters or fewer")
        String details,

        @Size(max = 255, message = "Contact must be 255 characters or fewer")
        String contact,

        @NotNull(message = "Activity time is required")
        Instant occurredAt
) {
}