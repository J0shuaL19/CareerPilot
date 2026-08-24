package com.careerpilot.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record JobAttentionBulkSnoozeRequest(
        @NotEmpty(message = "At least one job is required")
        @Size(max = 100, message = "No more than 100 reminders can be updated at once")
        List<@NotNull(message = "Job id is required") @Positive(message = "Job id must be positive") Long> jobIds,
        @NotNull(message = "Snooze date is required")
        @Future(message = "Snooze date must be in the future")
        LocalDate snoozedUntil
) {
}
