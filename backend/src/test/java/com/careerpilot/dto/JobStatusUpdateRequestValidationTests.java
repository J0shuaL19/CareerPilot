package com.careerpilot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.JobStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class JobStatusUpdateRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidStatus() {
        JobStatusUpdateRequest request = new JobStatusUpdateRequest(JobStatus.APPLIED);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingStatus() {
        JobStatusUpdateRequest request = new JobStatusUpdateRequest(null);

        assertThat(validator.validate(request)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Job status is required");
    }
}
