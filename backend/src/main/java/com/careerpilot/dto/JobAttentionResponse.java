package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;
import java.time.Instant;

public record JobAttentionResponse(
        Long jobId,
        String company,
        String jobTitle,
        JobStatus status,
        Instant lastActivityAt,
        long daysWithoutActivity
) {
}
