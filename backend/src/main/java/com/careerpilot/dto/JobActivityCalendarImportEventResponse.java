package com.careerpilot.dto;

import com.careerpilot.model.JobActivityType;
import java.time.Instant;
import java.util.List;

public record JobActivityCalendarImportEventResponse(
        int eventNumber,
        String title,
        String details,
        String contact,
        Instant occurredAt,
        JobActivityType suggestedType,
        boolean importable,
        List<String> errors
) {
}