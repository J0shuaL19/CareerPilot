package com.careerpilot.dto;

public record DataTransferCountsResponse(
        long jobs,
        long resumes,
        long jobActivities,
        long matchAnalyses,
        long attentionSettings,
        long attentionEvents,
        long interviewPreparations
) {
    public long totalRecords() {
        return jobs
                + resumes
                + jobActivities
                + matchAnalyses
                + attentionSettings
                + attentionEvents
                + interviewPreparations;
    }
}
