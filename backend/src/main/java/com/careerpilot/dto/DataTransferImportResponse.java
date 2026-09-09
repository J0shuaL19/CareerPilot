package com.careerpilot.dto;

import java.time.Instant;

public record DataTransferImportResponse(
        Instant importedAt,
        DataTransferCountsResponse imported
) {
}
