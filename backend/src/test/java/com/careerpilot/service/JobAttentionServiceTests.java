package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobAttentionResponse;
import com.careerpilot.dto.JobAttentionSettingsResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobActivityLastTouchProjection;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobAttentionServiceTests {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActivityRepository jobActivityRepository;

    @Mock
    private JobAttentionSettingsService settingsService;

    @Mock
    private Clock clock;

    @InjectMocks
    private JobAttentionService jobAttentionService;

    @Test
    void appliesStageThresholdsAndReturnsMostOverdueJobsFirst() {
        Job applied = persistedJob(1L, "Older Co", JobStatus.APPLIED, "2026-08-01T12:00:00Z");
        Job interview = persistedJob(
                2L,
                "Boundary Co",
                JobStatus.INTERVIEW,
                "2026-08-10T12:00:00Z"
        );
        Job recent = persistedJob(3L, "Recent Co", JobStatus.OA, "2026-08-20T12:00:00Z");
        Job scheduled = persistedJob(
                4L,
                "Scheduled Co",
                JobStatus.INTERVIEW,
                "2026-08-01T12:00:00Z"
        );
        when(jobRepository.findAllByStatusInOrderByCreatedAtAsc(List.of(
                JobStatus.APPLIED,
                JobStatus.OA,
                JobStatus.INTERVIEW
        ))).thenReturn(List.of(applied, interview, recent, scheduled));
        when(jobActivityRepository.findLatestOccurredAtByJobIds(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(
                        lastTouch(1L, "2026-08-05T12:00:00Z"),
                        lastTouch(2L, "2026-08-16T12:00:00Z"),
                        lastTouch(3L, "2026-08-22T12:00:00Z"),
                        lastTouch(4L, "2026-08-25T12:00:00Z")
                ));
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(settingsService.getSettings())
                .thenReturn(new JobAttentionSettingsResponse(14, 2, 7));

        List<JobAttentionResponse> responses = jobAttentionService.getJobsNeedingAttention();

        assertThat(responses).extracting(JobAttentionResponse::company)
                .containsExactly("Older Co", "Boundary Co");
        assertThat(responses).extracting(JobAttentionResponse::daysWithoutActivity)
                .containsExactly(18L, 7L);
        assertThat(responses).extracting(JobAttentionResponse::thresholdDays)
                .containsExactly(14, 7);
    }

    @Test
    void usesJobCreationTimeWhenThereAreNoActivities() {
        Job applied = persistedJob(1L, "OpenAI", JobStatus.APPLIED, "2026-08-10T12:00:00Z");
        when(jobRepository.findAllByStatusInOrderByCreatedAtAsc(List.of(
                JobStatus.APPLIED,
                JobStatus.OA,
                JobStatus.INTERVIEW
        ))).thenReturn(List.of(applied));
        when(jobActivityRepository.findLatestOccurredAtByJobIds(List.of(1L)))
                .thenReturn(List.of());
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(settingsService.getSettings())
                .thenReturn(new JobAttentionSettingsResponse(7, 7, 7));

        List<JobAttentionResponse> responses = jobAttentionService.getJobsNeedingAttention();

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.jobId()).isEqualTo(1L);
            assertThat(response.lastActivityAt()).isEqualTo(Instant.parse("2026-08-10T12:00:00Z"));
            assertThat(response.daysWithoutActivity()).isEqualTo(13L);
            assertThat(response.thresholdDays()).isEqualTo(7);
        });
    }

    @Test
    void excludesFutureSnoozesAndRestoresRemindersOnSelectedDate() {
        Job futureSnooze = persistedJob(
                1L,
                "Future Snooze Co",
                JobStatus.APPLIED,
                "2026-08-01T12:00:00Z"
        );
        futureSnooze.snoozeAttentionUntil(LocalDate.parse("2026-08-24"));
        Job dueToday = persistedJob(
                2L,
                "Due Today Co",
                JobStatus.APPLIED,
                "2026-08-01T12:00:00Z"
        );
        dueToday.snoozeAttentionUntil(LocalDate.parse("2026-08-23"));
        when(jobRepository.findAllByStatusInOrderByCreatedAtAsc(List.of(
                JobStatus.APPLIED,
                JobStatus.OA,
                JobStatus.INTERVIEW
        ))).thenReturn(List.of(futureSnooze, dueToday));
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(jobActivityRepository.findLatestOccurredAtByJobIds(List.of(2L)))
                .thenReturn(List.of());
        when(settingsService.getSettings())
                .thenReturn(new JobAttentionSettingsResponse(7, 7, 7));

        List<JobAttentionResponse> responses = jobAttentionService.getJobsNeedingAttention();

        assertThat(responses).extracting(JobAttentionResponse::company)
                .containsExactly("Due Today Co");
    }

    @Test
    void returnsEmptyWithoutQueryingActivitiesOrSettingsWhenThereAreNoActiveJobs() {
        when(jobRepository.findAllByStatusInOrderByCreatedAtAsc(List.of(
                JobStatus.APPLIED,
                JobStatus.OA,
                JobStatus.INTERVIEW
        ))).thenReturn(List.of());

        assertThat(jobAttentionService.getJobsNeedingAttention()).isEmpty();

        verifyNoInteractions(jobActivityRepository, settingsService, clock);
    }

    private static Job persistedJob(
            Long id,
            String company,
            JobStatus status,
            String createdAt
    ) {
        Job job = new Job(company, "Engineer", "Description", null);
        job.updateStatus(status);
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "createdAt", Instant.parse(createdAt));
        return job;
    }

    private static JobActivityLastTouchProjection lastTouch(Long jobId, String occurredAt) {
        return new TestLastTouch(jobId, Instant.parse(occurredAt));
    }

    private record TestLastTouch(Long jobId, Instant lastOccurredAt)
            implements JobActivityLastTouchProjection {

        @Override
        public Long getJobId() {
            return jobId;
        }

        @Override
        public Instant getLastOccurredAt() {
            return lastOccurredAt;
        }
    }
}
