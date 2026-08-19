package com.careerpilot.service;

import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
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
                job.getCreatedAt()
        );
    }
}
