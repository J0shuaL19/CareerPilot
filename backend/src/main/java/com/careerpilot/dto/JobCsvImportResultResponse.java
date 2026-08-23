package com.careerpilot.dto;

public record JobCsvImportResultResponse(
        int imported,
        int skippedDuplicates,
        int skippedInvalid
) {
}
