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
        Instant completedAt,
        Integer preparationCompletedSections,
        Integer preparationTotalSections,
        Integer preparationProgressPercent
) {
    public ScheduledJobActivityResponse(
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
        this(id, jobId, company, jobTitle, type, title, contact, occurredAt, completedAt,
                null, null, null);
    }
}
