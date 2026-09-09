package com.careerpilot.dto;

public record DataTransferCapabilitiesResponse(
        boolean importEnabled,
        int formatVersion,
        long maxImportFileSizeBytes
) {
}
