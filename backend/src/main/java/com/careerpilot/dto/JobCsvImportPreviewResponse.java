package com.careerpilot.dto;

import java.util.List;

public record JobCsvImportPreviewResponse(
        String filename,
        int totalRows,
        int validRows,
        int duplicateRows,
        int invalidRows,
        List<JobCsvImportRowResponse> rows
) {
}
