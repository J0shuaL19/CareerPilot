package com.careerpilot.dto;

import java.time.Instant;

public record DataTransferPreviewResponse(
        String filename,
        int formatVersion,
        Instant exportedAt,
        DataTransferCountsResponse incoming,
        DataTransferCountsResponse existing,
        boolean willReplaceExistingData
) {
}
