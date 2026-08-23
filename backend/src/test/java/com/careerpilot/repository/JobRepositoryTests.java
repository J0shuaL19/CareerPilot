package com.careerpilot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class JobRepositoryTests {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void savesAndFindsJob() {
        Job job = new Job(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "https://example.com/jobs/1"
        );

        Job savedJob = jobRepository.saveAndFlush(job);

        assertThat(savedJob.getId()).isNotNull();
        assertThat(savedJob.getCreatedAt()).isNotNull();
        assertThat(savedJob.getStatus()).isEqualTo(JobStatus.SAVED);
        assertThat(jobRepository.findById(savedJob.getId())).contains(savedJob);
    }

    @Test
    void findsNewestJobsFirst() {
        Job olderJob = new Job("Company A", "Backend Engineer", "Description A", null);
        Job newerJob = new Job("Company B", "Full Stack Engineer", "Description B", null);
        ReflectionTestUtils.setField(olderJob, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(newerJob, "createdAt", Instant.parse("2026-01-02T00:00:00Z"));
        jobRepository.saveAllAndFlush(List.of(olderJob, newerJob));

        List<Job> jobs = jobRepository.findAllByOrderByCreatedAtDesc();

        assertThat(jobs).extracting(Job::getCompany)
                .containsExactly("Company B", "Company A");
    }

    @Test
    void findsJobsCreatedWithinRangeNewestFirst() {
        Job outsideRange = new Job("Company A", "Backend Engineer", "Description A", null);
        Job firstInRange = new Job("Company B", "Platform Engineer", "Description B", null);
        Job newestInRange = new Job("Company C", "Full Stack Engineer", "Description C", null);
        ReflectionTestUtils.setField(outsideRange, "createdAt", Instant.parse("2026-05-01T00:00:00Z"));
        ReflectionTestUtils.setField(firstInRange, "createdAt", Instant.parse("2026-06-01T00:00:00Z"));
        ReflectionTestUtils.setField(newestInRange, "createdAt", Instant.parse("2026-07-01T00:00:00Z"));
        jobRepository.saveAllAndFlush(List.of(outsideRange, firstInRange, newestInRange));

        List<Job> jobs = jobRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                Instant.parse("2026-05-25T00:00:00Z")
        );

        assertThat(jobs).extracting(Job::getCompany)
                .containsExactly("Company C", "Company B");
    }

    @Test
    void detectsDuplicateCompanyAndTitleIgnoringCase() {
        jobRepository.saveAndFlush(new Job(
                "OpenAI",
                "Software Engineer",
                "Description",
                null
        ));

        assertThat(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase(
                "openai",
                "SOFTWARE ENGINEER"
        )).isTrue();
        assertThat(jobRepository.existsByCompanyIgnoreCaseAndTitleIgnoreCase(
                "OpenAI",
                "Product Manager"
        )).isFalse();
    }
}
