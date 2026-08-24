package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;
import jakarta.validation.constraints.Size;

public record JobActivityCompletionRequest(
        @Size(max = 2000, message = "Completion note must be 2000 characters or fewer")
        String note,
        JobStatus jobStatus
) {
}