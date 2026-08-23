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

    @Test
    void findsUpcomingReminderTypesWithinTimeWindow() {
        Job job = jobRepository.save(new Job("OpenAI", "Engineer", "Description", null));
        jobActivityRepository.saveAllAndFlush(List.of(
                activity(job, JobActivityType.INTERVIEW, "Interview", "2026-08-23T12:00:00Z"),
                activity(job, JobActivityType.FOLLOW_UP, "Follow-up", "2026-08-25T12:00:00Z"),
                activity(job, JobActivityType.NOTE, "Future note", "2026-08-24T12:00:00Z"),
                activity(job, JobActivityType.INTERVIEW, "Past interview", "2026-08-20T12:00:00Z"),
                activity(job, JobActivityType.INTERVIEW, "Later interview", "2026-09-10T12:00:00Z")
        ));

        List<JobActivity> activities = jobActivityRepository
                .findAllByTypeInAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
                        List.of(JobActivityType.INTERVIEW, JobActivityType.FOLLOW_UP),
                        Instant.parse("2026-08-22T12:00:00Z"),
                        Instant.parse("2026-09-05T12:00:00Z")
                );

        assertThat(activities).extracting(JobActivity::getTitle)
                .containsExactly("Interview", "Follow-up");
    }

    @Test
    void findsLatestActivityTimeForEachJob() {
        Job firstJob = jobRepository.save(new Job("OpenAI", "Engineer", "Description", null));
        Job secondJob = jobRepository.save(new Job("Example", "Designer", "Description", null));
        jobActivityRepository.saveAllAndFlush(List.of(
                activity(firstJob, "Applied", "2026-08-10T12:00:00Z"),
                activity(firstJob, "Follow-up", "2026-08-18T12:00:00Z"),
                activity(secondJob, "Interview", "2026-08-20T12:00:00Z")
        ));

        List<JobActivityLastTouchProjection> lastTouches = jobActivityRepository
                .findLatestOccurredAtByJobIds(List.of(firstJob.getId(), secondJob.getId()));

        assertThat(lastTouches).anySatisfy(lastTouch -> {
            assertThat(lastTouch.getJobId()).isEqualTo(firstJob.getId());
            assertThat(lastTouch.getLastOccurredAt())
                    .isEqualTo(Instant.parse("2026-08-18T12:00:00Z"));
        }).anySatisfy(lastTouch -> {
            assertThat(lastTouch.getJobId()).isEqualTo(secondJob.getId());
            assertThat(lastTouch.getLastOccurredAt())
                    .isEqualTo(Instant.parse("2026-08-20T12:00:00Z"));
        });
    }

    private static JobActivity activity(Job job, String title, String occurredAt) {
        return activity(job, JobActivityType.NOTE, title, occurredAt);
    }

    private static JobActivity activity(
            Job job,
            JobActivityType type,
            String title,
            String occurredAt
    ) {
        return new JobActivity(
                job,
                type,
                title,
                null,
                null,
                Instant.parse(occurredAt)
        );
    }
}
