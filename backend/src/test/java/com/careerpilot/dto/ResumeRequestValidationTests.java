package com.careerpilot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResumeRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        ResumeRequest request = new ResumeRequest(
                "Backend Engineer Resume",
                "Experienced Java engineer focused on reliable backend systems."
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFields() {
        ResumeRequest request = new ResumeRequest(" ", null);

        Set<ConstraintViolation<ResumeRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "content");
    }

    @Test
    void rejectsNameThatExceedsDatabaseLimit() {
        ResumeRequest request = new ResumeRequest(
                "a".repeat(256),
                "Experienced Java engineer."
        );

        assertThat(validator.validate(request)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Resume name must be 255 characters or fewer");
    }
}
