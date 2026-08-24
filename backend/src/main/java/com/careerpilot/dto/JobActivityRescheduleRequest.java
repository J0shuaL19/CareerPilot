package com.careerpilot.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record JobActivityRescheduleRequest(
        @NotNull(message = "New activity time is required")
        Instant occurredAt
) {
}