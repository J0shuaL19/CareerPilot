package com.careerpilot.service;

import com.careerpilot.dto.JobAttentionBulkClearRequest;
import com.careerpilot.dto.JobAttentionBulkRestoreRequest;
import com.careerpilot.dto.JobAttentionBulkSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRestoreRequest;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.dto.JobStatusUpdateRequest;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.repository.JobAttentionEventRepository;
import com.careerpilot.repository.JobRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobAttentionEventRepository jobAttentionEventRepository;

    public JobService(
            JobRepository jobRepository,
            JobAttentionEventRepository jobAttentionEventRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobAttentionEventRepository = jobAttentionEventRepository;
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
        LocalDate previousDate = job.getAttentionSnoozedUntil();
        JobAttentionEventAction action = previousDate == null
                ? JobAttentionEventAction.SNOOZED
                : JobAttentionEventAction.RESCHEDULED;
        job.snoozeAttentionUntil(request.snoozedUntil());
        jobAttentionEventRepository.save(new JobAttentionEvent(
                job,
                action,
                previousDate,
                request.snoozedUntil()
        ));
        return toResponse(job);
    }

    @Transactional
    public JobResponse clearAttentionSnooze(Long id) {
        Job job = findJob(id);
        LocalDate previousDate = job.getAttentionSnoozedUntil();
        job.clearAttentionSnooze();
        if (previousDate != null) {
            jobAttentionEventRepository.save(new JobAttentionEvent(
                    job,
                    JobAttentionEventAction.RESUMED,
                    previousDate,
                    null
            ));
        }
        return toResponse(job);
    }

    @Transactional
    public JobResponse restoreAttentionSnooze(
            Long id,
            JobAttentionSnoozeRestoreRequest request
    ) {
        Job job = findJob(id);
        LocalDate previousDate = job.getAttentionSnoozedUntil();
        if (request.snoozedUntil() == null) {
            job.clearAttentionSnooze();
        } else {
            job.snoozeAttentionUntil(request.snoozedUntil());
        }
        if (previousDate != null || request.snoozedUntil() != null) {
            jobAttentionEventRepository.save(new JobAttentionEvent(
                    job,
                    JobAttentionEventAction.RESTORED,
                    previousDate,
                    request.snoozedUntil()
            ));
        }
        return toResponse(job);
    }

    @Transactional
    public List<JobResponse> snoozeAttention(JobAttentionBulkSnoozeRequest request) {
        List<Job> jobs = findJobs(request.jobIds());
        List<JobAttentionEvent> events = new ArrayList<>();
        for (Job job : jobs) {
            LocalDate previousDate = job.getAttentionSnoozedUntil();
            JobAttentionEventAction action = previousDate == null
                    ? JobAttentionEventAction.SNOOZED
                    : JobAttentionEventAction.RESCHEDULED;
            job.snoozeAttentionUntil(request.snoozedUntil());
            events.add(new JobAttentionEvent(
                    job,
                    action,
                    previousDate,
                    request.snoozedUntil()
            ));
        }
        jobAttentionEventRepository.saveAll(events);
        return jobs.stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public List<JobResponse> clearAttentionSnooze(JobAttentionBulkClearRequest request) {
        List<Job> jobs = findJobs(request.jobIds());
        List<JobAttentionEvent> events = new ArrayList<>();
        for (Job job : jobs) {
            LocalDate previousDate = job.getAttentionSnoozedUntil();
            job.clearAttentionSnooze();
            if (previousDate != null) {
                events.add(new JobAttentionEvent(
                        job,
                        JobAttentionEventAction.RESUMED,
                        previousDate,
                        null
                ));
            }
        }
        jobAttentionEventRepository.saveAll(events);
        return jobs.stream().map(JobService::toResponse).toList();
    }

    @Transactional
    public List<JobResponse> restoreAttentionSnoozes(JobAttentionBulkRestoreRequest request) {
        List<Job> jobs = findJobs(request.reminders().stream()
                .map(reminder -> reminder.jobId())
                .toList());
        List<JobAttentionEvent> events = new ArrayList<>();
        for (int index = 0; index < jobs.size(); index++) {
            Job job = jobs.get(index);
            LocalDate previousDate = job.getAttentionSnoozedUntil();
            LocalDate restoredDate = request.reminders().get(index).snoozedUntil();
            job.snoozeAttentionUntil(restoredDate);
            events.add(new JobAttentionEvent(
                    job,
                    JobAttentionEventAction.RESTORED,
                    previousDate,
                    restoredDate
            ));
        }
        jobAttentionEventRepository.saveAll(events);
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
