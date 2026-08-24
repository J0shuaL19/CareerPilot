package com.careerpilot.dto;

import com.careerpilot.model.JobActivityType;
import java.time.Instant;

public record ScheduledJobActivityResponse(
        Long id,
        Long jobId,
        String company,
        String jobTitle,
        JobActivityType type,
        String title,
        String contact,
        Instant occurredAt,
        Instant completedAt
) {
}
