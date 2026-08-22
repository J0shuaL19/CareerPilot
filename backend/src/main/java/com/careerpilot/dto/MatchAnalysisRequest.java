package com.careerpilot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatchAnalysisRequest(
        @NotNull(message = "Job id is required")
        @Positive(message = "Job id must be positive")
        Long jobId,

        @NotNull(message = "Resume id is required")
        @Positive(message = "Resume id must be positive")
        Long resumeId
) {
}
