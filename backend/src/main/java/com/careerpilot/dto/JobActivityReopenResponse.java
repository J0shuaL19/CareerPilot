package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;

public record JobActivityReopenResponse(
        JobActivityResponse activity,
        JobStatus jobStatus,
        boolean jobStatusRestored
) {
}