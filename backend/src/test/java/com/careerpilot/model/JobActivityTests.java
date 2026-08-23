package com.careerpilot.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobActivityTests {

    @Test
    void createsActivityAndSetsCreationTime() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        Instant occurredAt = Instant.parse("2026-08-25T18:00:00Z");
        JobActivity activity = new JobActivity(
                job,
                JobActivityType.INTERVIEW,
                "Technical interview",
                "System design round",
                "Alex Chen",
                occurredAt
        );

        activity.setCreationTime();

        assertThat(activity.getJob()).isSameAs(job);
        assertThat(activity.getType()).isEqualTo(JobActivityType.INTERVIEW);
        assertThat(activity.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(activity.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsMissingRequiredValues() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);

        assertThatNullPointerException()
                .isThrownBy(() -> new JobActivity(
                        job,
                        null,
                        "Interview",
                        null,
                        null,
                        Instant.now()
                ))
                .withMessage("Activity type is required");
    }
}
