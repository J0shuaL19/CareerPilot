package com.careerpilot.dto;

import com.careerpilot.model.JobActivityType;
import java.time.Instant;

public record JobActivityResponse(
        Long id,
        Long jobId,
        JobActivityType type,
        String title,
        String details,
        String contact,
        Instant occurredAt,
        Instant completedAt,
        String completionNote,
        Instant createdAt
) {
}
