package com.careerpilot.controller;

import com.careerpilot.dto.JobActivityRequest;
import com.careerpilot.dto.JobActivityResponse;
import com.careerpilot.service.JobActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs/{jobId}/activities")
public class JobActivityController {

    private final JobActivityService jobActivityService;

    public JobActivityController(JobActivityService jobActivityService) {
        this.jobActivityService = jobActivityService;
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

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long jobId,
            @PathVariable Long activityId
    ) {
        jobActivityService.deleteActivity(jobId, activityId);
        return ResponseEntity.noContent().build();
    }
}
