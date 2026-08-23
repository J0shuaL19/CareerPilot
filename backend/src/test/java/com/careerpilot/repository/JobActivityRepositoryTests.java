package com.careerpilot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class JobActivityRepositoryTests {

    @Autowired
    private JobActivityRepository jobActivityRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void listsActivitiesForJobNewestOccurrenceFirst() {
        Job targetJob = jobRepository.save(new Job("OpenAI", "Engineer", "Description", null));
        Job otherJob = jobRepository.save(new Job("Example", "Designer", "Description", null));
        jobActivityRepository.saveAllAndFlush(List.of(
                activity(targetJob, "Applied", "2026-08-20T12:00:00Z"),
                activity(targetJob, "Interview", "2026-08-22T12:00:00Z"),
                activity(otherJob, "Other note", "2026-08-23T12:00:00Z")
        ));

        List<JobActivity> activities =
                jobActivityRepository.findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(
                        targetJob.getId()
                );

        assertThat(activities).extracting(JobActivity::getTitle)
                .containsExactly("Interview", "Applied");
    }

    @Test
    void findsActivityOnlyWithinOwningJob() {
        Job owner = jobRepository.save(new Job("OpenAI", "Engineer", "Description", null));
        Job other = jobRepository.save(new Job("Example", "Designer", "Description", null));
        JobActivity saved = jobActivityRepository.saveAndFlush(
                activity(owner, "Interview", "2026-08-22T12:00:00Z")
        );

        assertThat(jobActivityRepository.findByIdAndJob_Id(saved.getId(), owner.getId())).isPresent();
        assertThat(jobActivityRepository.findByIdAndJob_Id(saved.getId(), other.getId())).isEmpty();
    }

    private static JobActivity activity(Job job, String title, String occurredAt) {
        return new JobActivity(
                job,
                JobActivityType.NOTE,
                title,
                null,
                null,
                Instant.parse(occurredAt)
        );
    }
}
