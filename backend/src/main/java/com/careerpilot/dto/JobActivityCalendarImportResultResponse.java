package com.careerpilot.dto;

public record JobActivityCalendarImportResultResponse(
        int imported,
        int skippedDuplicates
) {
}