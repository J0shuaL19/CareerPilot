package com.careerpilot.dto;

import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.model.JobStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CareerPilotArchive(
        String format,
        Integer version,
        Instant exportedAt,
        ArchiveData data
) {
    public record ArchiveData(
            List<JobItem> jobs,
            List<ResumeItem> resumes,
            List<JobActivityItem> jobActivities,
            List<MatchAnalysisItem> matchAnalyses,
            AttentionSettingsItem attentionSettings,
            List<AttentionEventItem> attentionEvents,
            List<InterviewPreparationItem> interviewPreparations
    ) {
    }

    public record JobItem(
            Long sourceId,
            String company,
            String title,
            String description,
            String jobUrl,
            JobStatus status,
            Instant createdAt,
            LocalDate attentionSnoozedUntil
    ) {
    }

    public record ResumeItem(
            Long sourceId,
            String name,
            String content,
            Instant createdAt
    ) {
    }

    public record JobActivityItem(
            Long sourceId,
            Long jobSourceId,
            JobActivityType type,
            String title,
            String details,
            String contact,
            Instant occurredAt,
            Instant completedAt,
            String completionNote,
            JobStatus completionPreviousJobStatus,
            JobStatus completionAppliedJobStatus,
            Instant createdAt
    ) {
    }

    public record MatchAnalysisItem(
            Long sourceId,
            Long jobSourceId,
            Long resumeSourceId,
            int matchScore,
            String summary,
            String strengths,
            String gaps,
            String recommendations,
            String modelName,
            Instant createdAt
    ) {
    }

    public record AttentionSettingsItem(
            int appliedDays,
            int onlineAssessmentDays,
            int interviewDays,
            Instant updatedAt
    ) {
    }

    public record AttentionEventItem(
            Long sourceId,
            Long jobSourceId,
            JobAttentionEventAction action,
            LocalDate previousSnoozedUntil,
            LocalDate newSnoozedUntil,
            Instant createdAt
    ) {
    }

    public record InterviewPreparationItem(
            Long sourceId,
            Long activitySourceId,
            String companyResearch,
            boolean companyResearchDone,
            String rolePriorities,
            boolean rolePrioritiesDone,
            String starStories,
            boolean starStoriesDone,
            String questionsToAsk,
            boolean questionsToAskDone,
            Instant updatedAt
    ) {
    }
}
