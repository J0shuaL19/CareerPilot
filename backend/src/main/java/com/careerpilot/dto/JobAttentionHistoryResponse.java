package com.careerpilot.dto;

import com.careerpilot.model.JobAttentionEventAction;
import java.time.Instant;
import java.time.LocalDate;

public record JobAttentionHistoryResponse(
        Long id,
        Long jobId,
        String company,
        String jobTitle,
        JobAttentionEventAction action,
        LocalDate previousSnoozedUntil,
        LocalDate newSnoozedUntil,
        Instant createdAt
) {
}