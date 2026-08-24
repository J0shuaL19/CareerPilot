package com.careerpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobActivityCalendarImportRequest(
        @NotEmpty(message = "At least one calendar event is required")
        @Size(max = 100, message = "At most 100 calendar events can be imported at once")
        List<@Valid JobActivityCalendarImportItemRequest> events
) {
}