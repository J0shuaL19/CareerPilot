package com.careerpilot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        JobRequest request = new JobRequest(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFields() {
        JobRequest request = new JobRequest(" ", "", null, null);

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("company", "title", "description");
    }

    @Test
    void rejectsInvalidJobUrl() {
        JobRequest request = new JobRequest(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "not-a-url"
        );

        assertThat(validator.validate(request)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Job URL must be a valid URL");
    }

    @Test
    void rejectsFieldsThatExceedDatabaseLimits() {
        JobRequest request = new JobRequest(
                "a".repeat(256),
                "b".repeat(256),
                "Description",
                "https://example.com/" + "c".repeat(2048)
        );

        assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("company", "title", "jobUrl");
    }
}
