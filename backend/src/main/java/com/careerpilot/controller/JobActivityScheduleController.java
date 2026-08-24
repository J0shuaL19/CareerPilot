package com.careerpilot.controller;

import com.careerpilot.dto.JobActivityCalendarExportRequest;
import com.careerpilot.dto.ScheduledJobActivityResponse;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.service.JobActivityCalendarFile;
import com.careerpilot.service.JobActivityCalendarService;
import com.careerpilot.service.JobActivityService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities")
public class JobActivityScheduleController {

    private final JobActivityService jobActivityService;
    private final JobActivityCalendarService jobActivityCalendarService;

    public JobActivityScheduleController(
            JobActivityService jobActivityService,
            JobActivityCalendarService jobActivityCalendarService
    ) {
        this.jobActivityService = jobActivityService;
        this.jobActivityCalendarService = jobActivityCalendarService;
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

    @PostMapping("/calendar/export")
    public ResponseEntity<byte[]> exportCalendar(
            @Valid @RequestBody JobActivityCalendarExportRequest request
    ) {
        JobActivityCalendarFile file = jobActivityCalendarService.export(request.activityIds());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\""
                )
                .body(file.content());
    }
}