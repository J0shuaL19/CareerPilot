package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeRequest(
        @NotBlank(message = "Resume name is required")
        @Size(max = 255, message = "Resume name must be 255 characters or fewer")
        String name,

        @NotBlank(message = "Resume content is required")
        String content
) {
}
