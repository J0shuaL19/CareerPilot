package com.careerpilot.service;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobActivity;
import com.careerpilot.repository.JobActivityRepository;
import com.careerpilot.repository.JobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobActivityService {

    private final JobRepository jobRepository;
    private final JobActivityRepository jobActivityRepository;

    public JobActivityService(
            JobRepository jobRepository,
            JobActivityRepository jobActivityRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobActivityRepository = jobActivityRepository;
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
    public void deleteActivity(Long jobId, Long activityId) {
        findJob(jobId);
        JobActivity activity = jobActivityRepository.findByIdAndJob_Id(activityId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job activity", activityId));
        jobActivityRepository.delete(activity);
    }

    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
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
}
