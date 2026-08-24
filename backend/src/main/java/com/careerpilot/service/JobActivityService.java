package com.careerpilot.service;

import com.careerpilot.dto.JobActivityCompletionRequest;
import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.dto.JobActivityReopenResponse;
import com.careerpilot.dto.JobActivityRescheduleRequest;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.exception.JobActivityCompletionException;
import com.careerpilot.exception.JobActivityRescheduleException;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobAttentionEventRepository;
import com.careerpilot.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private final JobAttentionEventRepository jobAttentionEventRepository;
    private final Clock clock;

    public JobActivityService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository,
            JobAttentionEventRepository jobAttentionEventRepository,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
        this.jobAttentionEventRepository = jobAttentionEventRepository;
        this.clock = clock;
    }

    @Transactional
    public JobActivityResponse createActivity(Long jobId, JobActivityRequest request) {
        Job job = findJob(jobId);
        clearAttentionSnooze(job);
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

    @Transactional
    public JobActivityResponse completeActivity(
            Long jobId,
            Long activityId,
            JobActivityCompletionRequest request
    ) {
        Job job = findJob(jobId);
        JobActivity activity = findActivity(jobId, activityId);
        if (!REMINDER_TYPES.contains(activity.getType())) {
            throw new JobActivityCompletionException(
                    "Only interviews and follow-ups can be completed."
            );
        }
        if (activity.getCompletedAt() != null) {
            throw new JobActivityCompletionException("Activity is already completed.");
        }

        JobStatus previousJobStatus = request.jobStatus() != null
                && request.jobStatus() != job.getStatus()
                ? job.getStatus()
                : null;
        JobStatus appliedJobStatus = previousJobStatus == null ? null : request.jobStatus();
        activity.complete(
                clock.instant(),
                normalizeOptional(request.note()),
                previousJobStatus,
                appliedJobStatus
        );
        if (appliedJobStatus != null) {
            job.updateStatus(appliedJobStatus);
        }
        return toResponse(activity);
    }

    @Transactional
    public JobActivityReopenResponse reopenActivity(Long jobId, Long activityId) {
        Job job = findJob(jobId);
        JobActivity activity = findActivity(jobId, activityId);
        if (activity.getCompletedAt() == null) {
            throw new JobActivityCompletionException("Activity is not completed.");
        }

        boolean jobStatusRestored = activity.getCompletionPreviousJobStatus() != null
                && activity.getCompletionAppliedJobStatus() == job.getStatus();
        if (jobStatusRestored) {
            job.updateStatus(activity.getCompletionPreviousJobStatus());
        }
        activity.reopen();
        return new JobActivityReopenResponse(
                toResponse(activity),
                job.getStatus(),
                jobStatusRestored
        );
    }

    @Transactional
    public JobActivityResponse rescheduleActivity(
            Long jobId,
            Long activityId,
            JobActivityRescheduleRequest request
    ) {
        Job job = findJob(jobId);
        JobActivity activity = findActivity(jobId, activityId);
        if (!REMINDER_TYPES.contains(activity.getType())) {
            throw new JobActivityRescheduleException(
                    "Only interviews and follow-ups can be rescheduled."
            );
        }
        if (activity.getCompletedAt() != null) {
            throw new JobActivityRescheduleException(
                    "Completed activities must be reopened before rescheduling."
            );
        }
        if (!request.occurredAt().isAfter(clock.instant())) {
            throw new JobActivityRescheduleException(
                    "New activity time must be in the future."
            );
        }

        clearAttentionSnooze(job);
        activity.reschedule(request.occurredAt());
        return toResponse(activity);
    }

    @Transactional(readOnly = true)
    public List<UpcomingJobActivityResponse> getOverdueActivities() {
        return jobActivityRepository
                .findAllByTypeInAndCompletedAtIsNullAndOccurredAtBeforeOrderByOccurredAtAscCreatedAtAsc(
                        REMINDER_TYPES,
                        clock.instant()
                )
                .stream()
                .map(JobActivityService::toUpcomingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UpcomingJobActivityResponse> getUpcomingActivities() {
        Instant start = clock.instant();
        Instant end = start.plus(UPCOMING_WINDOW_DAYS, ChronoUnit.DAYS);
        return jobActivityRepository
                .findAllByTypeInAndCompletedAtIsNullAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
                        REMINDER_TYPES,
                        start,
                        end
                )
                .stream()
                .map(JobActivityService::toUpcomingResponse)
                .toList();
    }

    private void clearAttentionSnooze(Job job) {
        LocalDate previousSnoozeDate = job.getAttentionSnoozedUntil();
        job.clearAttentionSnooze();
        if (previousSnoozeDate != null) {
            jobAttentionEventRepository.save(new JobAttentionEvent(
                    job,
                    JobAttentionEventAction.CLEARED_BY_ACTIVITY,
                    previousSnoozeDate,
                    null
            ));
        }
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
                activity.getCompletedAt(),
                activity.getCompletionNote(),
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
