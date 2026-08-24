package com.careerpilot.controller;

import com.careerpilot.dto.JobAttentionHistoryResponse;
import com.careerpilot.dto.JobAttentionResponse;
import com.careerpilot.service.JobAttentionHistoryService;
import com.careerpilot.service.JobAttentionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities")
public class JobAttentionController {

    private final JobAttentionService jobAttentionService;
    private final JobAttentionHistoryService jobAttentionHistoryService;

    public JobAttentionController(
            JobAttentionService jobAttentionService,
            JobAttentionHistoryService jobAttentionHistoryService
    ) {
        this.jobAttentionService = jobAttentionService;
        this.jobAttentionHistoryService = jobAttentionHistoryService;
    }

    @GetMapping("/needs-attention")
    public List<JobAttentionResponse> getJobsNeedingAttention() {
        return jobAttentionService.getJobsNeedingAttention();
    }

    @GetMapping("/attention-history")
    public List<JobAttentionHistoryResponse> getAttentionHistory() {
        return jobAttentionHistoryService.getRecentHistory();
    }
}
