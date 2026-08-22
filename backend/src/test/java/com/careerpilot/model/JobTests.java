package com.careerpilot.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class JobTests {

    @Test
    void newJobStartsWithSavedStatus() {
        Job job = new Job(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1"
        );

        assertThat(job.getCompany()).isEqualTo("OpenAI");
        assertThat(job.getTitle()).isEqualTo("Software Engineer");
        assertThat(job.getDescription()).isEqualTo("Build reliable products.");
        assertThat(job.getJobUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(job.getStatus()).isEqualTo(JobStatus.SAVED);
        assertThat(job.getId()).isNull();
        assertThat(job.getCreatedAt()).isNull();
    }

    @Test
    void persistenceCallbackSetsCreationTime() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);

        job.setCreationTime();

        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    void updatesStatus() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);

        job.updateStatus(JobStatus.INTERVIEW);

        assertThat(job.getStatus()).isEqualTo(JobStatus.INTERVIEW);
    }

    @Test
    void rejectsNullStatus() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);

        assertThatNullPointerException()
                .isThrownBy(() -> job.updateStatus(null))
                .withMessage("Job status is required");
    }

    @Test
    void updatesJobDetailsWithoutChangingStatus() {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        job.updateStatus(JobStatus.INTERVIEW);

        job.updateDetails(
                "Anthropic",
                "Senior Engineer",
                "Build safe AI systems.",
                "https://example.com/jobs/2"
        );

        assertThat(job.getCompany()).isEqualTo("Anthropic");
        assertThat(job.getTitle()).isEqualTo("Senior Engineer");
        assertThat(job.getDescription()).isEqualTo("Build safe AI systems.");
        assertThat(job.getJobUrl()).isEqualTo("https://example.com/jobs/2");
        assertThat(job.getStatus()).isEqualTo(JobStatus.INTERVIEW);
    }
}
