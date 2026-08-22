package com.careerpilot.ai;

public record GeneratedMatchAnalysis(
        int matchScore,
        String summary,
        String strengths,
        String gaps,
        String recommendations,
        String modelName
) {

    public GeneratedMatchAnalysis {
        if (matchScore < 0 || matchScore > 100) {
            throw new IllegalArgumentException("Match score must be between 0 and 100");
        }
        requireText(summary, "Summary");
        requireText(strengths, "Strengths");
        requireText(gaps, "Gaps");
        requireText(recommendations, "Recommendations");
        requireText(modelName, "Model name");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
