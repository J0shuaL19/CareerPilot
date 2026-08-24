package com.careerpilot.dto;

public record JobAttentionSettingsResponse(
        int appliedDays,
        int onlineAssessmentDays,
        int interviewDays
) {
}
