package com.careerpilot.dto;

import java.time.Instant;

public record MatchAnalysisResponse(
        Long id,
        Long jobId,
        String company,
        String jobTitle,
        Long resumeId,
        String resumeName,
        int matchScore,
        String summary,
        String strengths,
        String gaps,
        String recommendations,
        String modelName,
        Instant createdAt
) {
}
