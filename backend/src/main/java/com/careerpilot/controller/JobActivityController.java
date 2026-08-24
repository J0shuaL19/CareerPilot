package com.careerpilot.controller;

import com.careerpilot.dto.JobActivityCompletionRequest;
import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.service.JobActivityCalendarFile;
import com.careerpilot.service.JobActivityCalendarService;
import com.careerpilot.service.JobActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs/{jobId}/activities")
public class JobActivityController {

    private final JobActivityService jobActivityService;
    private final JobActivityCalendarService jobActivityCalendarService;

    public JobActivityController(
            JobActivityService jobActivityService,
            JobActivityCalendarService jobActivityCalendarService
    ) {
        this.jobActivityService = jobActivityService;
        this.jobActivityCalendarService = jobActivityCalendarService;
    }

    @PostMapping
    public ResponseEntity<JobActivityResponse> createActivity(
            @PathVariable Long jobId,
            @Valid @RequestBody JobActivityRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobActivityService.createActivity(jobId, request));
    }

    @GetMapping
    public List<JobActivityResponse> getActivities(@PathVariable Long jobId) {
        return jobActivityService.getActivities(jobId);
    }

    @PutMapping("/{activityId}")
    public JobActivityResponse updateActivity(
            @PathVariable Long jobId,
            @PathVariable Long activityId,
            @Valid @RequestBody JobActivityRequest request
    ) {
        return jobActivityService.updateActivity(jobId, activityId, request);
    }

    @PutMapping("/{activityId}/complete")
    public JobActivityResponse completeActivity(
            @PathVariable Long jobId,
            @PathVariable Long activityId,
            @Valid @RequestBody JobActivityCompletionRequest request
    ) {
        return jobActivityService.completeActivity(jobId, activityId, request);
    }

    @GetMapping("/{activityId}/calendar")
    public ResponseEntity<byte[]> exportActivityCalendar(
            @PathVariable Long jobId,
            @PathVariable Long activityId
    ) {
        JobActivityCalendarFile file = jobActivityCalendarService.export(jobId, activityId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\""
                )
                .body(file.content());
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long jobId,
            @PathVariable Long activityId
    ) {
        jobActivityService.deleteActivity(jobId, activityId);
        return ResponseEntity.noContent().build();
    }
}
