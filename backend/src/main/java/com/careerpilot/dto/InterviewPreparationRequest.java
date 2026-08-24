package com.careerpilot.dto;

import jakarta.validation.constraints.Size;

public record InterviewPreparationRequest(
        @Size(max = 5000, message = "Company research must be 5000 characters or fewer")
        String companyResearch,
        boolean companyResearchDone,

        @Size(max = 5000, message = "Role priorities must be 5000 characters or fewer")
        String rolePriorities,
        boolean rolePrioritiesDone,

        @Size(max = 5000, message = "STAR stories must be 5000 characters or fewer")
        String starStories,
        boolean starStoriesDone,

        @Size(max = 5000, message = "Questions to ask must be 5000 characters or fewer")
        String questionsToAsk,
        boolean questionsToAskDone
) {
}
