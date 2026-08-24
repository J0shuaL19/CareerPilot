package com.careerpilot.service;

import com.careerpilot.dto.JobAttentionSettingsRequest;
import com.careerpilot.dto.JobAttentionSettingsResponse;
import com.careerpilot.model.JobAttentionSettings;
import com.careerpilot.repository.JobAttentionSettingsRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobAttentionSettingsService {

    private static final long SETTINGS_ID = 1L;
    private static final int DEFAULT_DAYS = 7;

    private final JobAttentionSettingsRepository repository;
    private final Clock clock;

    public JobAttentionSettingsService(JobAttentionSettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public JobAttentionSettingsResponse getSettings() {
        return repository.findById(SETTINGS_ID)
                .map(JobAttentionSettingsService::toResponse)
                .orElseGet(JobAttentionSettingsService::defaultResponse);
    }

    @Transactional
    public JobAttentionSettingsResponse updateSettings(JobAttentionSettingsRequest request) {
        Instant now = clock.instant();
        JobAttentionSettings settings = repository.findById(SETTINGS_ID)
                .orElseGet(() -> new JobAttentionSettings(
                        SETTINGS_ID,
                        DEFAULT_DAYS,
                        DEFAULT_DAYS,
                        DEFAULT_DAYS,
                        now
                ));
        settings.update(
                request.appliedDays(),
                request.onlineAssessmentDays(),
                request.interviewDays(),
                now
        );
        return toResponse(repository.save(settings));
    }

    private static JobAttentionSettingsResponse toResponse(JobAttentionSettings settings) {
        return new JobAttentionSettingsResponse(
                settings.getAppliedDays(),
                settings.getOnlineAssessmentDays(),
                settings.getInterviewDays()
        );
    }

    private static JobAttentionSettingsResponse defaultResponse() {
        return new JobAttentionSettingsResponse(DEFAULT_DAYS, DEFAULT_DAYS, DEFAULT_DAYS);
    }
}
