package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;
import java.time.Instant;
import java.time.LocalDate;

public record JobResponse(
        Long id,
        String company,
        String title,
        String description,
        String jobUrl,
        JobStatus status,
        Instant createdAt,
        LocalDate attentionSnoozedUntil
) {
}
