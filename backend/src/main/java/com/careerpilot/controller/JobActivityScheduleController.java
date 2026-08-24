package com.careerpilot.controller;

import com.careerpilot.dto.ScheduledJobActivityResponse;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.service.JobActivityService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities")
public class JobActivityScheduleController {

    private final JobActivityService jobActivityService;

    public JobActivityScheduleController(JobActivityService jobActivityService) {
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

    @GetMapping("/calendar")
    public List<ScheduledJobActivityResponse> getCalendarActivities(
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {
        return jobActivityService.getCalendarActivities(start, end);
    }
}