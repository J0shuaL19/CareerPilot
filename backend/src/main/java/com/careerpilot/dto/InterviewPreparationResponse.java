package com.careerpilot.dto;

import java.time.Instant;

public record InterviewPreparationResponse(
        Long activityId,
        String companyResearch,
        boolean companyResearchDone,
        String rolePriorities,
        boolean rolePrioritiesDone,
        String starStories,
        boolean starStoriesDone,
        String questionsToAsk,
        boolean questionsToAskDone,
        int completedSections,
        int totalSections,
        int progressPercent,
        Instant updatedAt
) {
}
