package com.careerpilot.service;

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
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobActivityService {

    private static final long UPCOMING_WINDOW_DAYS = 14;
    private static final List<JobActivityType> REMINDER_TYPES = List.of(
            JobActivityType.INTERVIEW,
            JobActivityType.FOLLOW_UP
    );

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;
    private final Clock clock;

    public JobActivityService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.clock = clock;
    }

    @Transactional
    public JobActivityResponse createActivity(Long jobId, JobActivityRequest request) {
        Job job = findJob(jobId);
        JobActivity activity = new JobActivity(
                job,
                request.type(),
                request.title().trim(),
                normalizeOptional(request.details()),
                normalizeOptional(request.contact()),
                request.occurredAt()
        );
        return toResponse(jobActivityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public List<JobActivityResponse> getActivities(Long jobId) {
        findJob(jobId);
        return jobActivityRepository.findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(jobId)
                .stream()
                .map(JobActivityService::toResponse)
                .toList();
    }

    @Transactional
    public JobActivityResponse updateActivity(
            Long jobId,
            Long activityId,
            JobActivityRequest request
    ) {
        findJob(jobId);
        JobActivity activity = findActivity(jobId, activityId);
        activity.update(
                request.type(),
                request.title().trim(),
                normalizeOptional(request.details()),
                normalizeOptional(request.contact()),
                request.occurredAt()
        );
        return toResponse(activity);
    }

    @Transactional
    public void deleteActivity(Long jobId, Long activityId) {
        findJob(jobId);
        JobActivity activity = findActivity(jobId, activityId);
        jobActivityRepository.delete(activity);
    }

    @Transactional(readOnly = true)
    public List<UpcomingJobActivityResponse> getUpcomingActivities() {
        Instant start = clock.instant();
        Instant end = start.plus(UPCOMING_WINDOW_DAYS, ChronoUnit.DAYS);
        return jobActivityRepository
                .findAllByTypeInAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
                        REMINDER_TYPES,
                        start,
                        end
                )
                .stream()
                .map(JobActivityService::toUpcomingResponse)
                .toList();
    }

    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    private JobActivity findActivity(Long jobId, Long activityId) {
        return jobActivityRepository.findByIdAndJob_Id(activityId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job activity", activityId));
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static JobActivityResponse toResponse(JobActivity activity) {
        return new JobActivityResponse(
                activity.getId(),
                activity.getJob().getId(),
                activity.getType(),
                activity.getTitle(),
                activity.getDetails(),
                activity.getContact(),
                activity.getOccurredAt(),
                activity.getCreatedAt()
        );
    }

    private static UpcomingJobActivityResponse toUpcomingResponse(JobActivity activity) {
        return new UpcomingJobActivityResponse(
                activity.getId(),
                activity.getJob().getId(),
                activity.getJob().getCompany(),
                activity.getJob().getTitle(),
                activity.getType(),
                activity.getTitle(),
                activity.getContact(),
                activity.getOccurredAt()
        );
    }
}
