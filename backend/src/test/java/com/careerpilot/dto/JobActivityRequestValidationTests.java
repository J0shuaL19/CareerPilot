package com.careerpilot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.JobActivityType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobActivityRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        JobActivityRequest request = new JobActivityRequest(
                JobActivityType.INTERVIEW,
                "Technical interview",
                "Discussed system design.",
                "Alex Chen",
                Instant.parse("2026-08-25T18:00:00Z")
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFields() {
        JobActivityRequest request = new JobActivityRequest(null, " ", null, null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("type", "title", "occurredAt");
    }

    @Test
    void rejectsOversizedOptionalFields() {
        JobActivityRequest request = new JobActivityRequest(
                JobActivityType.NOTE,
                "Note",
                "a".repeat(5001),
                "a".repeat(256),
                Instant.now()
        );

        assertThat(validator.validate(request)).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Activity details must be 5000 characters or fewer",
                        "Contact must be 255 characters or fewer"
                );
    }
}
