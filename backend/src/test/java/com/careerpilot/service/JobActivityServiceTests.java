package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobActivityServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActivityRepository jobActivityRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private JobActivityService jobActivityService;

    @Test
    void createsNormalizedActivityForJob() {
        Job job = persistedJob(1L);
        job.snoozeAttentionUntil(LocalDate.parse("2026-08-30"));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.save(any(JobActivity.class))).thenAnswer(invocation -> {
            JobActivity activity = invocation.getArgument(0);
            ReflectionTestUtils.setField(activity, "id", 2L);
            ReflectionTestUtils.setField(activity, "createdAt", Instant.parse("2026-08-22T12:00:00Z"));
            return activity;
        });

        JobActivityResponse response = jobActivityService.createActivity(
                1L,
                new JobActivityRequest(
                        JobActivityType.INTERVIEW,
                        "  Technical interview  ",
                        "  System design round  ",
                        "  Alex Chen  ",
                        Instant.parse("2026-08-25T18:00:00Z")
                )
        );

        ArgumentCaptor<JobActivity> captor = ArgumentCaptor.forClass(JobActivity.class);
        verify(jobActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Technical interview");
        assertThat(captor.getValue().getDetails()).isEqualTo("System design round");
        assertThat(captor.getValue().getContact()).isEqualTo("Alex Chen");
        assertThat(job.getAttentionSnoozedUntil()).isNull();
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.jobId()).isEqualTo(1L);
    }

    @Test
    void normalizesBlankOptionalValuesToNull() {
        Job job = persistedJob(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.save(any(JobActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobActivityResponse response = jobActivityService.createActivity(
                1L,
                new JobActivityRequest(
                        JobActivityType.NOTE,
                        "Note",
                        " ",
                        " ",
                        Instant.now()
                )
        );

        assertThat(response.details()).isNull();
        assertThat(response.contact()).isNull();
    }

    @Test
    void listsActivitiesInRepositoryOrder() {
        Job job = persistedJob(1L);
        JobActivity interview = persistedActivity(2L, job, "Interview", "2026-08-22T12:00:00Z");
        JobActivity applied = persistedActivity(1L, job, "Applied", "2026-08-20T12:00:00Z");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(1L))
                .thenReturn(List.of(interview, applied));

        List<JobActivityResponse> responses = jobActivityService.getActivities(1L);

        assertThat(responses).extracting(JobActivityResponse::title)
                .containsExactly("Interview", "Applied");
    }

    @Test
    void updatesNormalizedActivityOwnedByJob() {
        Job job = persistedJob(1L);
        JobActivity activity = persistedActivity(2L, job, "Interview", "2026-08-22T12:00:00Z");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(2L, 1L)).thenReturn(Optional.of(activity));

        JobActivityResponse response = jobActivityService.updateActivity(
                1L,
                2L,
                new JobActivityRequest(
                        JobActivityType.FOLLOW_UP,
                        "  Send thank-you note  ",
                        "  Mention platform discussion  ",
                        "  Alex Chen  ",
                        Instant.parse("2026-08-26T18:00:00Z")
                )
        );

        assertThat(response.type()).isEqualTo(JobActivityType.FOLLOW_UP);
        assertThat(response.title()).isEqualTo("Send thank-you note");
        assertThat(response.details()).isEqualTo("Mention platform discussion");
        assertThat(response.contact()).isEqualTo("Alex Chen");
        assertThat(response.occurredAt()).isEqualTo(Instant.parse("2026-08-26T18:00:00Z"));
    }

    @Test
    void deletesActivityOwnedByJob() {
        Job job = persistedJob(1L);
        JobActivity activity = persistedActivity(2L, job, "Interview", "2026-08-22T12:00:00Z");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(2L, 1L)).thenReturn(Optional.of(activity));

        jobActivityService.deleteActivity(1L, 2L);

        verify(jobActivityRepository).delete(activity);
    }

    @Test
    void throwsWhenJobDoesNotExist() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobActivityService.getActivities(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found with id: 999");
    }

    @Test
    void throwsWhenActivityDoesNotBelongToJob() {
        Job job = persistedJob(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobActivityService.deleteActivity(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job activity not found with id: 99");
    }

    @Test
    void rejectsUpdatingActivityOwnedByAnotherJob() {
        Job job = persistedJob(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobActivityRepository.findByIdAndJob_Id(99L, 1L)).thenReturn(Optional.empty());

        JobActivityRequest request = new JobActivityRequest(
                JobActivityType.NOTE,
                "Updated note",
                null,
                null,
                Instant.parse("2026-08-26T18:00:00Z")
        );

        assertThatThrownBy(() -> jobActivityService.updateActivity(1L, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job activity not found with id: 99");
    }

    @Test
    void returnsUpcomingReminderActivitiesInRepositoryOrder() {
        Instant now = Instant.parse("2026-08-22T12:00:00Z");
        Job job = persistedJob(1L);
        JobActivity interview = persistedActivity(
                2L,
                job,
                JobActivityType.INTERVIEW,
                "Technical interview",
                "2026-08-23T12:00:00Z"
        );
        JobActivity followUp = persistedActivity(
                3L,
                job,
                JobActivityType.FOLLOW_UP,
                "Recruiter follow-up",
                "2026-08-25T12:00:00Z"
        );
        when(clock.instant()).thenReturn(now);
        when(jobActivityRepository
                .findAllByTypeInAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
                        List.of(JobActivityType.INTERVIEW, JobActivityType.FOLLOW_UP),
                        now,
                        Instant.parse("2026-09-05T12:00:00Z")
                ))
                .thenReturn(List.of(interview, followUp));

        List<UpcomingJobActivityResponse> responses =
                jobActivityService.getUpcomingActivities();

        assertThat(responses).extracting(UpcomingJobActivityResponse::title)
                .containsExactly("Technical interview", "Recruiter follow-up");
        assertThat(responses.getFirst().company()).isEqualTo("OpenAI");
        assertThat(responses.getFirst().jobTitle()).isEqualTo("Engineer");
    }

    private static Job persistedJob(Long id) {
        Job job = new Job("OpenAI", "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static JobActivity persistedActivity(
            Long id,
            Job job,
            String title,
            String occurredAt
    ) {
        return persistedActivity(id, job, JobActivityType.NOTE, title, occurredAt);
    }

    private static JobActivity persistedActivity(
            Long id,
            Job job,
            JobActivityType type,
            String title,
            String occurredAt
    ) {
        JobActivity activity = new JobActivity(
                job,
                type,
                title,
                null,
                null,
                Instant.parse(occurredAt)
        );
        ReflectionTestUtils.setField(activity, "id", id);
        ReflectionTestUtils.setField(activity, "createdAt", Instant.parse(occurredAt));
        return activity;
    }
}
