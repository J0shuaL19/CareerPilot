package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;
import jakarta.validation.constraints.NotNull;

public record JobStatusUpdateRequest(
        @NotNull(message = "Job status is required")
        JobStatus status
) {
}
