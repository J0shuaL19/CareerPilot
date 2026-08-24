package com.careerpilot.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record JobAttentionSnoozeRestoreItem(
        @NotNull(message = "Job id is required")
        @Positive(message = "Job id must be positive")
        Long jobId,
        @NotNull(message = "Snooze date is required")
        @Future(message = "Snooze date must be in the future")
        LocalDate snoozedUntil
) {
}
