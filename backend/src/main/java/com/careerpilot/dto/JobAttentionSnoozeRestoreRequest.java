package com.careerpilot.dto;

import jakarta.validation.constraints.Future;
import java.time.LocalDate;

public record JobAttentionSnoozeRestoreRequest(
        @Future(message = "Snooze date must be in the future")
        LocalDate snoozedUntil
) {
}