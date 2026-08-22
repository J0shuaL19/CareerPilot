package com.careerpilot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MatchAnalysisRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        MatchAnalysisRequest request = new MatchAnalysisRequest(1L, 2L);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingIds() {
        MatchAnalysisRequest request = new MatchAnalysisRequest(null, null);

        Set<ConstraintViolation<MatchAnalysisRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("jobId", "resumeId");
    }

    @Test
    void rejectsNonPositiveIds() {
        MatchAnalysisRequest request = new MatchAnalysisRequest(0L, -1L);

        assertThat(validator.validate(request)).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Job id must be positive", "Resume id must be positive");
    }
}
