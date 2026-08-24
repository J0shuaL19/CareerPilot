package com.careerpilot.service;

import com.careerpilot.dto.JobAttentionBulkClearRequest;
import com.careerpilot.dto.JobAttentionBulkRestoreRequest;
import com.careerpilot.dto.JobAttentionBulkSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRequest;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.dto.JobStatusUpdateRequest;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.repository.JobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public JobResponse createJob(JobRequest request) {
        Job job = new Job(
                request.company().trim(),
                request.title().trim(),
                request.description().trim(),
                normalizeOptional(request.jobUrl())
        );

        return toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(JobService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long id) {
        return jobRepository.findById(id)
                .map(JobService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    @Transactional
    public JobResponse updateJobStatus(Long id, JobStatusUpdateRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));

        job.updateStatus(request.status());
        return toResponse(job);
    }

    @Transactional
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = findJob(id);
        job.updateDetails(
                request.company().trim(),
                request.title().trim(),
                request.description().trim(),
                normalizeOptional(request.jobUrl())
        );
        return toResponse(job);
    }

    @Transactional
    public JobResponse snoozeAttention(Long id, JobAttentionSnoozeRequest request) {
        Job job = findJob(id);
        job.snoozeAttentionUntil(request.snoozedUntil());
        return toResponse(job);
    }

    @Transactional
    public JobResponse clearAttentionSnooze(Long id) {
        Job job = findJob(id);
        job.clearAttentionSnooze();
        return toResponse(job);
    }

    @Transactional
    public List<JobResponse> snoozeAttention(JobAttentionBulkSnoozeRequest request) {
        List<Job> jobs = findJobs(request.jobIds());
        jobs.forEach(job -> job.snoozeAttentionUntil(request.snoozedUntil()));
        return jobs.stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public List<JobResponse> clearAttentionSnooze(JobAttentionBulkClearRequest request) {
        List<Job> jobs = findJobs(request.jobIds());
        jobs.forEach(Job::clearAttentionSnooze);
        return jobs.stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public List<JobResponse> restoreAttentionSnoozes(JobAttentionBulkRestoreRequest request) {
        List<Job> jobs = findJobs(request.reminders().stream()
                .map(reminder -> reminder.jobId())
                .toList());
        for (int index = 0; index < jobs.size(); index++) {
            jobs.get(index).snoozeAttentionUntil(
                    request.reminders().get(index).snoozedUntil()
            );
        }
        return jobs.stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public void deleteJob(Long id) {
        jobRepository.delete(findJob(id));
    }

    private List<Job> findJobs(List<Long> jobIds) {
        return jobIds.stream().map(this::findJob).toList();
    }

    private Job findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getCompany(),
                job.getTitle(),
                job.getDescription(),
                job.getJobUrl(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getAttentionSnoozedUntil()
        );
    }
}
