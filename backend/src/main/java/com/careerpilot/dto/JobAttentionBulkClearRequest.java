package com.careerpilot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobAttentionBulkClearRequest(
        @NotEmpty(message = "At least one job is required")
        @Size(max = 100, message = "No more than 100 reminders can be resumed at once")
        List<@NotNull(message = "Job id is required") @Positive(message = "Job id must be positive") Long> jobIds
) {
}
