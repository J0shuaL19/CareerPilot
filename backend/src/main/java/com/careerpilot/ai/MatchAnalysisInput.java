package com.careerpilot.ai;

public record MatchAnalysisInput(
        String company,
        String jobTitle,
        String jobDescription,
        String resumeName,
        String resumeContent
) {
}
