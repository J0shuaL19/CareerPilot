package com.careerpilot.dto;

import com.careerpilot.model.JobStatus;
import java.util.List;

public record JobCsvImportRowResponse(
        int rowNumber,
        String company,
        String title,
        String description,
        String jobUrl,
        JobStatus status,
        JobCsvImportRowState state,
        List<String> errors
) {
}
