package com.careerpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record JobAttentionSettingsRequest(
        @Min(value = 1, message = "Applied reminder must be at least 1 day")
        @Max(value = 90, message = "Applied reminder must be at most 90 days")
        int appliedDays,

        @Min(value = 1, message = "Online assessment reminder must be at least 1 day")
        @Max(value = 90, message = "Online assessment reminder must be at most 90 days")
        int onlineAssessmentDays,

        @Min(value = 1, message = "Interview reminder must be at least 1 day")
        @Max(value = 90, message = "Interview reminder must be at most 90 days")
        int interviewDays
) {
}
