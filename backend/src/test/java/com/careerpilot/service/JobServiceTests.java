package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import com.careerpilot.repository.JobRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobServiceTests {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void createsJobFromNormalizedRequest() {
        JobRequest request = new JobRequest(
                "  OpenAI  ",
                "  Software Engineer  ",
                "  Build reliable products.  ",
                "  https://example.com/jobs/1  "
        );
        Instant createdAt = Instant.parse("2026-08-18T12:00:00Z");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedJob, "id", 1L);
            ReflectionTestUtils.setField(savedJob, "createdAt", createdAt);
            return savedJob;
        });

        JobResponse response = jobService.createJob(request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job persistedJob = jobCaptor.getValue();
        assertThat(persistedJob.getCompany()).isEqualTo("OpenAI");
        assertThat(persistedJob.getTitle()).isEqualTo("Software Engineer");
        assertThat(persistedJob.getDescription()).isEqualTo("Build reliable products.");
        assertThat(persistedJob.getJobUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(JobStatus.SAVED);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void convertsBlankOptionalUrlToNull() {
        JobRequest request = new JobRequest("OpenAI", "Engineer", "Description", "  ");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.createJob(request);

        assertThat(response.jobUrl()).isNull();
    }

    @Test
    void returnsJobsInRepositoryOrder() {
        Job newestJob = persistedJob(2L, "Company B", "2026-08-18T12:00:00Z");
        Job olderJob = persistedJob(1L, "Company A", "2026-08-17T12:00:00Z");
        when(jobRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newestJob, olderJob));

        List<JobResponse> responses = jobService.getJobs();

        assertThat(responses).extracting(JobResponse::id).containsExactly(2L, 1L);
        assertThat(responses).extracting(JobResponse::company)
                .containsExactly("Company B", "Company A");
    }

    private static Job persistedJob(Long id, String company, String createdAt) {
        Job job = new Job(company, "Engineer", "Description", null);
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "createdAt", Instant.parse(createdAt));
        return job;
    }
}
