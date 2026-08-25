package com.careerpilot.service;

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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobAttentionService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(
            JobStatus.APPLIED,
            JobStatus.OA,
            JobStatus.INTERVIEW
    );

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final JobAttentionSettingsService settingsService;
    private final Clock clock;

    public JobAttentionService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            JobAttentionSettingsService settingsService,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<JobAttentionResponse> getJobsNeedingAttention() {
        List<Job> activeJobs = jobRepository.findAllByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES);
        if (activeJobs.isEmpty()) {
            return List.of();
        }

        Instant now = clock.instant();
        LocalDate today = now.atZone(clock.getZone()).toLocalDate();
        List<Job> unsnoozedJobs = activeJobs.stream()
                .filter(job -> job.getAttentionSnoozedUntil() == null
                        || !job.getAttentionSnoozedUntil().isAfter(today))
                .toList();
        if (unsnoozedJobs.isEmpty()) {
            return List.of();
        }

        Map<Long, JobActivityLastTouchProjection> lastTouches = jobActivityRepository
                .findLatestOccurredAtByJobIds(unsnoozedJobs.stream().map(Job::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        JobActivityLastTouchProjection::getJobId,
                        Function.identity()
                ));
        JobAttentionSettingsResponse settings = settingsService.getSettings();

        return unsnoozedJobs.stream()
                .map(job -> toResponse(
                        job,
                        lastTouches.get(job.getId()),
                        now,
                        thresholdFor(job.getStatus(), settings)
                ))
                .filter(response -> response.daysWithoutActivity() >= response.thresholdDays())
                .sorted(Comparator
                        .comparingLong(JobAttentionService::daysOverdue)
                        .reversed()
                        .thenComparing(JobAttentionResponse::lastActivityAt)
                        .thenComparing(JobAttentionResponse::jobId))
                .toList();
    }

    private static JobAttentionResponse toResponse(
            Job job,
            JobActivityLastTouchProjection lastTouch,
            Instant now,
            int thresholdDays
    ) {
        Instant lastActivityAt = lastTouch == null
                ? job.getCreatedAt()
                : lastTouch.getLastOccurredAt();
        return new JobAttentionResponse(
                job.getId(),
                job.getCompany(),
                job.getTitle(),
                job.getStatus(),
                lastActivityAt,
                ChronoUnit.DAYS.between(lastActivityAt, now),
                thresholdDays
        );
    }

    private static int thresholdFor(
            JobStatus status,
            JobAttentionSettingsResponse settings
    ) {
        return switch (status) {
            case APPLIED -> settings.appliedDays();
            case OA -> settings.onlineAssessmentDays();
            case INTERVIEW -> settings.interviewDays();
            default -> throw new IllegalArgumentException("Inactive job status: " + status);
        };
    }

    private static long daysOverdue(JobAttentionResponse response) {
        return response.daysWithoutActivity() - response.thresholdDays();
    }
}
