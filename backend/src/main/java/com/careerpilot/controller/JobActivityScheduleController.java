package com.careerpilot.controller;

import com.careerpilot.dto.JobActivityCalendarExportRequest;
import com.careerpilot.dto.JobActivityCalendarImportPreviewResponse;
import com.careerpilot.dto.JobActivityCalendarImportRequest;
import com.careerpilot.dto.JobActivityCalendarImportResultResponse;
import com.careerpilot.dto.ScheduledJobActivityResponse;
import com.careerpilot.dto.UpcomingJobActivityResponse;
import com.careerpilot.service.JobActivityCalendarFile;
import com.careerpilot.service.JobActivityCalendarImportService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/job-activities")
public class JobActivityScheduleController {

    private final JobActivityService jobActivityService;
    private final JobActivityCalendarService jobActivityCalendarService;
    private final JobActivityCalendarImportService jobActivityCalendarImportService;

    public JobActivityScheduleController(
            JobActivityService jobActivityService,
            JobActivityCalendarService jobActivityCalendarService,
            JobActivityCalendarImportService jobActivityCalendarImportService
    ) {
        this.jobActivityService = jobActivityService;
        this.jobActivityCalendarService = jobActivityCalendarService;
        this.jobActivityCalendarImportService = jobActivityCalendarImportService;
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

    @PostMapping(path = "/calendar/import/preview", consumes = "multipart/form-data")
    public JobActivityCalendarImportPreviewResponse previewCalendarImport(
            @RequestPart("file") MultipartFile file
    ) {
        return jobActivityCalendarImportService.preview(file);
    }

    @PostMapping("/calendar/import")
    public JobActivityCalendarImportResultResponse importCalendar(
            @Valid @RequestBody JobActivityCalendarImportRequest request
    ) {
        return jobActivityCalendarImportService.importEvents(request.events());
    }
}