package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.DashboardStatsRange;
import com.careerpilot.dto.DashboardStatsResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTests {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobActivityRepository jobActivityRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DashboardStatsService dashboardStatsService;

    @Test
    void calculatesFunnelFromCurrentStatusesAndRecordedActivities() {
        Job savedWithApplication = persistedJob(1L, JobStatus.SAVED);
        Job applied = persistedJob(2L, JobStatus.APPLIED);
        Job rejectedAfterInterview = persistedJob(3L, JobStatus.REJECTED);
        Job offer = persistedJob(4L, JobStatus.OFFER);
        List<Job> jobs = List.of(savedWithApplication, applied, rejectedAfterInterview, offer);
        when(clock.instant()).thenReturn(NOW);
        when(jobRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                Instant.parse("2026-05-25T12:00:00Z")
        )).thenReturn(jobs);
        when(jobActivityRepository.findAllByJob_IdInAndTypeIn(
                List.of(1L, 2L, 3L, 4L),
                List.of(JobActivityType.APPLICATION, JobActivityType.INTERVIEW)
        )).thenReturn(List.of(
                activity(savedWithApplication, JobActivityType.APPLICATION),
                activity(rejectedAfterInterview, JobActivityType.INTERVIEW)
        ));

        DashboardStatsResponse stats = dashboardStatsService.getStats(
                DashboardStatsRange.LAST_90_DAYS
        );

        assertThat(stats.range()).isEqualTo(DashboardStatsRange.LAST_90_DAYS);
        assertThat(stats.from()).isEqualTo(Instant.parse("2026-05-25T12:00:00Z"));
        assertThat(stats.to()).isEqualTo(NOW);
        assertThat(stats.trackedJobs()).isEqualTo(4);
        assertThat(stats.applications()).isEqualTo(4);
        assertThat(stats.interviews()).isEqualTo(2);
        assertThat(stats.offers()).isEqualTo(1);
        assertThat(stats.applicationRate()).isEqualTo(100);
        assertThat(stats.interviewRate()).isEqualTo(50);
        assertThat(stats.offerRate()).isEqualTo(50);
    }

    @Test
    void usesAllJobsAndRoundsPercentagesForAllTimeStats() {
        Job saved = persistedJob(1L, JobStatus.SAVED);
        Job appliedOne = persistedJob(2L, JobStatus.APPLIED);
        Job appliedTwo = persistedJob(3L, JobStatus.OA);
        when(clock.instant()).thenReturn(NOW);
        when(jobRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(saved, appliedOne, appliedTwo));
        when(jobActivityRepository.findAllByJob_IdInAndTypeIn(
                List.of(1L, 2L, 3L),
                List.of(JobActivityType.APPLICATION, JobActivityType.INTERVIEW)
        )).thenReturn(List.of());

        DashboardStatsResponse stats = dashboardStatsService.getStats(DashboardStatsRange.ALL_TIME);

        assertThat(stats.from()).isNull();
        assertThat(stats.trackedJobs()).isEqualTo(3);
        assertThat(stats.applications()).isEqualTo(2);
        assertThat(stats.applicationRate()).isEqualTo(67);
        assertThat(stats.interviewRate()).isZero();
        assertThat(stats.offerRate()).isNull();
    }

    @Test
    void returnsEmptyStatsWithoutLoadingActivities() {
        when(clock.instant()).thenReturn(NOW);
        when(jobRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                Instant.parse("2026-07-24T12:00:00Z")
        )).thenReturn(List.of());

        DashboardStatsResponse stats = dashboardStatsService.getStats(
                DashboardStatsRange.LAST_30_DAYS
        );

        assertThat(stats.trackedJobs()).isZero();
        assertThat(stats.applications()).isZero();
        assertThat(stats.interviews()).isZero();
        assertThat(stats.offers()).isZero();
        assertThat(stats.applicationRate()).isNull();
        assertThat(stats.interviewRate()).isNull();
        assertThat(stats.offerRate()).isNull();
        verifyNoInteractions(jobActivityRepository);
    }

    private static Job persistedJob(Long id, JobStatus status) {
        Job job = new Job("Company " + id, "Engineer", "Description", null);
        job.updateStatus(status);
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "createdAt", NOW.minusSeconds(id * 60));
        return job;
    }

    private static JobActivity activity(Job job, JobActivityType type) {
        return new JobActivity(job, type, "Activity", null, null, NOW);
    }
}
