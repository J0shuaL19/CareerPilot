package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.JobAttentionSettingsRequest;
import com.careerpilot.dto.JobAttentionSettingsResponse;
import com.careerpilot.model.JobAttentionSettings;
import com.careerpilot.repository.JobAttentionSettingsRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobAttentionSettingsServiceTests {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");

    @Mock
    private JobAttentionSettingsRepository repository;

    private JobAttentionSettingsService service;

    @BeforeEach
    void setUp() {
        service = new JobAttentionSettingsService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void returnsPersistedSettings() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                new JobAttentionSettings(1L, 10, 5, 3, NOW)
        ));

        assertThat(service.getSettings())
                .isEqualTo(new JobAttentionSettingsResponse(10, 5, 3));
    }

    @Test
    void returnsBackwardCompatibleDefaultsWhenSettingsAreMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.getSettings())
                .isEqualTo(new JobAttentionSettingsResponse(7, 7, 7));
    }

    @Test
    void updatesAndPersistsAllStageThresholds() {
        JobAttentionSettings settings = new JobAttentionSettings(1L, 7, 7, 7, NOW.minusSeconds(60));
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(repository.save(any(JobAttentionSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobAttentionSettingsResponse response = service.updateSettings(
                new JobAttentionSettingsRequest(12, 5, 2)
        );

        assertThat(response).isEqualTo(new JobAttentionSettingsResponse(12, 5, 2));
        assertThat(settings.getAppliedDays()).isEqualTo(12);
        assertThat(settings.getOnlineAssessmentDays()).isEqualTo(5);
        assertThat(settings.getInterviewDays()).isEqualTo(2);
        assertThat(settings.getUpdatedAt()).isEqualTo(NOW);
    }
}
