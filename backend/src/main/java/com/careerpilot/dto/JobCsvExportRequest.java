package com.careerpilot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobCsvExportRequest(
        @NotEmpty(message = "At least one job is required")
        @Size(max = 10_000, message = "No more than 10000 jobs can be exported at once")
        List<@NotNull(message = "Job id is required") @Positive(message = "Job id must be positive") Long> jobIds
) {
}
