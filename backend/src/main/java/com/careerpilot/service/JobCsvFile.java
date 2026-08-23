package com.careerpilot.service;

public record JobCsvFile(
        String filename,
        byte[] content
) {
}
