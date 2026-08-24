package com.careerpilot.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record JobAttentionSnoozeRequest(
        @NotNull(message = "Snooze date is required")
        @Future(message = "Snooze date must be in the future")
        LocalDate snoozedUntil
) {
}
