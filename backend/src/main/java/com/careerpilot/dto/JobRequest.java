package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record JobRequest(
        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must be 255 characters or fewer")
        String company,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be 255 characters or fewer")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @Size(max = 2048, message = "Job URL must be 2048 characters or fewer")
        @URL(message = "Job URL must be a valid URL")
        String jobUrl
) {
}
