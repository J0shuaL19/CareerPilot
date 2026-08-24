package com.careerpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JobAttentionBulkRestoreRequest(
        @NotEmpty(message = "At least one reminder is required")
        @Size(max = 100, message = "No more than 100 reminders can be restored at once")
        List<@Valid JobAttentionSnoozeRestoreItem> reminders
) {
}
