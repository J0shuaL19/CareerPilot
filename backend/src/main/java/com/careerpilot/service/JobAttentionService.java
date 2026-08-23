package com.careerpilot.service;

import com.careerpilot.dto.JobAttentionResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobActivityLastTouchProjection;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
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

    static final long ATTENTION_THRESHOLD_DAYS = 7;
    private static final List<JobStatus> ACTIVE_STATUSES = List.of(
            JobStatus.APPLIED,
            JobStatus.OA,
            JobStatus.INTERVIEW
    );

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final Clock clock;

    public JobAttentionService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<JobAttentionResponse> getJobsNeedingAttention() {
        List<Job> activeJobs = jobRepository.findAllByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES);
        if (activeJobs.isEmpty()) {
            return List.of();
        }

        Map<Long, JobActivityLastTouchProjection> lastTouches = jobActivityRepository
                .findLatestOccurredAtByJobIds(activeJobs.stream().map(Job::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        JobActivityLastTouchProjection::getJobId,
                        Function.identity()
                ));
        Instant now = clock.instant();
        Instant cutoff = now.minus(ATTENTION_THRESHOLD_DAYS, ChronoUnit.DAYS);

        return activeJobs.stream()
                .map(job -> toResponse(job, lastTouches.get(job.getId()), now))
                .filter(response -> !response.lastActivityAt().isAfter(cutoff))
                .sorted(Comparator.comparing(JobAttentionResponse::lastActivityAt)
                        .thenComparing(JobAttentionResponse::jobId))
                .toList();
    }

    private static JobAttentionResponse toResponse(
            Job job,
            JobActivityLastTouchProjection lastTouch,
            Instant now
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
                ChronoUnit.DAYS.between(lastActivityAt, now)
        );
    }
}
