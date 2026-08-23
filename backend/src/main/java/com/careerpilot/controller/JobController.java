package com.careerpilot.controller;

import com.careerpilot.dto.JobCsvExportRequest;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.dto.JobStatusUpdateRequest;
import com.careerpilot.service.JobCsvExportService;
import com.careerpilot.service.JobCsvFile;
import com.careerpilot.service.JobService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobCsvExportService jobCsvExportService;

    public JobController(JobService jobService, JobCsvExportService jobCsvExportService) {
        this.jobService = jobService;
        this.jobCsvExportService = jobCsvExportService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @GetMapping
    public List<JobResponse> getJobs() {
        return jobService.getJobs();
    }

    @PostMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportJobs(@Valid @RequestBody JobCsvExportRequest request) {
        JobCsvFile file = jobCsvExportService.export(request.jobIds());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\""
                )
                .body(file.content());
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable Long id) {
        return jobService.getJob(id);
    }

    @PatchMapping("/{id}/status")
    public JobResponse updateJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobStatusUpdateRequest request
    ) {
        return jobService.updateJobStatus(id, request);
    }

    @PutMapping("/{id}")
    public JobResponse updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request
    ) {
        return jobService.updateJob(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
