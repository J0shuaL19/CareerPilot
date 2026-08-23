package com.careerpilot.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResumeTests {

    @Test
    void createsResumeFromNameAndContent() {
        Resume resume = new Resume(
                "Master Resume",
                "Joshua Liu\nSoftware Engineer\nJava, React, PostgreSQL"
        );

        assertThat(resume.getName()).isEqualTo("Master Resume");
        assertThat(resume.getContent()).contains("Software Engineer");
        assertThat(resume.getId()).isNull();
        assertThat(resume.getCreatedAt()).isNull();
    }

    @Test
    void persistenceCallbackSetsCreationTime() {
        Resume resume = new Resume("Master Resume", "Resume content");

        resume.setCreationTime();

        assertThat(resume.getCreatedAt()).isNotNull();
    }

    @Test
    void updatesResumeDetailsWithoutChangingCreationTime() {
        Resume resume = new Resume("Master Resume", "Original content");
        resume.setCreationTime();
        var createdAt = resume.getCreatedAt();

        resume.updateDetails("Backend Resume", "Updated content");

        assertThat(resume.getName()).isEqualTo("Backend Resume");
        assertThat(resume.getContent()).isEqualTo("Updated content");
        assertThat(resume.getCreatedAt()).isEqualTo(createdAt);
    }
}
