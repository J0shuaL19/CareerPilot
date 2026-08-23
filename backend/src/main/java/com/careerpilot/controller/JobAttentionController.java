package com.careerpilot.controller;

import com.careerpilot.dto.JobAttentionResponse;
import com.careerpilot.service.JobAttentionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities")
public class JobAttentionController {

    private final JobAttentionService jobAttentionService;

    public JobAttentionController(JobAttentionService jobAttentionService) {
        this.jobAttentionService = jobAttentionService;
    }

    @GetMapping("/needs-attention")
    public List<JobAttentionResponse> getJobsNeedingAttention() {
        return jobAttentionService.getJobsNeedingAttention();
    }
}
