package com.careerpilot.controller;

import com.careerpilot.dto.JobAttentionBulkClearRequest;
import com.careerpilot.dto.JobAttentionBulkRestoreRequest;
import com.careerpilot.dto.JobAttentionBulkSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRequest;
import com.careerpilot.dto.JobAttentionSnoozeRestoreRequest;
import com.careerpilot.dto.JobCsvExportRequest;
import com.careerpilot.dto.JobCsvImportPreviewResponse;
import com.careerpilot.dto.JobCsvImportResultResponse;
import com.careerpilot.dto.JobRequest;
import com.careerpilot.dto.JobResponse;
import com.careerpilot.dto.JobStatusUpdateRequest;
import com.careerpilot.service.JobCsvExportService;
import com.careerpilot.service.JobCsvFile;
import com.careerpilot.service.JobCsvImportService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobCsvExportService jobCsvExportService;
    private final JobCsvImportService jobCsvImportService;

    public JobController(
            JobService jobService,
            JobCsvExportService jobCsvExportService,
            JobCsvImportService jobCsvImportService
    ) {
        this.jobService = jobService;
        this.jobCsvExportService = jobCsvExportService;
        this.jobCsvImportService = jobCsvImportService;
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

    @PostMapping(path = "/import/preview", consumes = "multipart/form-data")
    public JobCsvImportPreviewResponse previewImport(@RequestPart("file") MultipartFile file) {
        return jobCsvImportService.preview(file);
    }

    @PostMapping(path = "/import", consumes = "multipart/form-data")
    public JobCsvImportResultResponse importJobs(@RequestPart("file") MultipartFile file) {
        return jobCsvImportService.importFile(file);
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

    @PutMapping("/{id}/attention-snooze")
    public JobResponse snoozeAttention(
            @PathVariable Long id,
            @Valid @RequestBody JobAttentionSnoozeRequest request
    ) {
        return jobService.snoozeAttention(id, request);
    }

    @PutMapping("/{id}/attention-snooze/restore")
    public JobResponse restoreAttentionSnooze(
            @PathVariable Long id,
            @Valid @RequestBody JobAttentionSnoozeRestoreRequest request
    ) {
        return jobService.restoreAttentionSnooze(id, request);
    }

    @PutMapping("/attention-snooze")
    public List<JobResponse> snoozeAttention(
            @Valid @RequestBody JobAttentionBulkSnoozeRequest request
    ) {
        return jobService.snoozeAttention(request);
    }

    @PostMapping("/attention-snooze/clear")
    public List<JobResponse> clearAttentionSnooze(
            @Valid @RequestBody JobAttentionBulkClearRequest request
    ) {
        return jobService.clearAttentionSnooze(request);
    }

    @PutMapping("/attention-snooze/restore")
    public List<JobResponse> restoreAttentionSnoozes(
            @Valid @RequestBody JobAttentionBulkRestoreRequest request
    ) {
        return jobService.restoreAttentionSnoozes(request);
    }

    @DeleteMapping("/{id}/attention-snooze")
    public JobResponse clearAttentionSnooze(@PathVariable Long id) {
        return jobService.clearAttentionSnooze(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
