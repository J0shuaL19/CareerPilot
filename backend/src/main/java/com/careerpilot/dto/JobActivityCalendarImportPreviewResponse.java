package com.careerpilot.dto;

import java.util.List;

public record JobActivityCalendarImportPreviewResponse(
        String filename,
        int totalEvents,
        int importableEvents,
        int invalidEvents,
        List<JobActivityCalendarImportEventResponse> events
) {
}