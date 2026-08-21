package com.careerpilot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.Resume;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class ResumeRepositoryTests {

    @Autowired
    private ResumeRepository resumeRepository;

    @Test
    void savesAndFindsResume() {
        Resume resume = new Resume(
                "Master Resume",
                "Joshua Liu\nSoftware Engineer\nJava, React, PostgreSQL"
        );

        Resume savedResume = resumeRepository.saveAndFlush(resume);

        assertThat(savedResume.getId()).isNotNull();
        assertThat(savedResume.getCreatedAt()).isNotNull();
        assertThat(resumeRepository.findById(savedResume.getId())).contains(savedResume);
    }

    @Test
    void findsNewestResumesFirst() {
        Resume olderResume = new Resume("Resume v1", "Older content");
        Resume newerResume = new Resume("Resume v2", "Newer content");
        ReflectionTestUtils.setField(olderResume, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(newerResume, "createdAt", Instant.parse("2026-01-02T00:00:00Z"));
        resumeRepository.saveAllAndFlush(List.of(olderResume, newerResume));

        List<Resume> resumes = resumeRepository.findAllByOrderByCreatedAtDesc();

        assertThat(resumes).extracting(Resume::getName)
                .containsExactly("Resume v2", "Resume v1");
    }
}
