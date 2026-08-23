package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Instant;
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

    @InjectMocks
    private JobActivityService jobActivityService;

    @Test
    void createsNormalizedActivityForJob() {
        Job job = persistedJob(1L);
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
        JobActivity activity = new JobActivity(
                job,
                JobActivityType.NOTE,
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
