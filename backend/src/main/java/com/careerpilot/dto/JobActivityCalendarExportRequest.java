package com.careerpilot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobActivityCalendarExportRequest(
        @NotEmpty(message = "At least one activity is required")
        @Size(max = 500, message = "No more than 500 activities can be exported at once")
        List<@NotNull(message = "Activity id is required")
                @Positive(message = "Activity id must be positive") Long> activityIds
) {
}
