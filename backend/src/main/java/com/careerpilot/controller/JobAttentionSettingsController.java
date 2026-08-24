package com.careerpilot.controller;

import com.careerpilot.dto.JobAttentionSettingsRequest;
import com.careerpilot.dto.JobAttentionSettingsResponse;
import com.careerpilot.service.JobAttentionSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-activities/attention-settings")
public class JobAttentionSettingsController {

    private final JobAttentionSettingsService service;

    public JobAttentionSettingsController(JobAttentionSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public JobAttentionSettingsResponse getSettings() {
        return service.getSettings();
    }

    @PutMapping
    public JobAttentionSettingsResponse updateSettings(
            @Valid @RequestBody JobAttentionSettingsRequest request
    ) {
        return service.updateSettings(request);
    }
}
