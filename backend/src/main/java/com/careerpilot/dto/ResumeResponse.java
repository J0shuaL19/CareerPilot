package com.careerpilot.dto;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        String name,
        String content,
        Instant createdAt
) {
}
