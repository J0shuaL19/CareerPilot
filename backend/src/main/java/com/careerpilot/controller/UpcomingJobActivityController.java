package com.careerpilot.controller;

import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.service.JobActivityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities")
public class UpcomingJobActivityController {

    private final JobActivityService jobActivityService;

    public UpcomingJobActivityController(JobActivityService jobActivityService) {
        this.jobActivityService = jobActivityService;
    }

    @GetMapping("/overdue")
    public List<UpcomingJobActivityResponse> getOverdueActivities() {
        return jobActivityService.getOverdueActivities();
    }

    @GetMapping("/upcoming")
    public List<UpcomingJobActivityResponse> getUpcomingActivities() {
        return jobActivityService.getUpcomingActivities();
    }
}
