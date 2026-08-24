package com.careerpilot.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobAttentionSettingsRequest;
import com.careerpilot.dto.JobAttentionSettingsResponse;
import com.careerpilot.service.JobAttentionSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobAttentionSettingsController.class)
class JobAttentionSettingsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobAttentionSettingsService service;

    @Test
    void returnsCurrentSettings() throws Exception {
        when(service.getSettings()).thenReturn(new JobAttentionSettingsResponse(10, 5, 3));

        mockMvc.perform(get("/api/job-activities/attention-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedDays").value(10))
                .andExpect(jsonPath("$.onlineAssessmentDays").value(5))
                .andExpect(jsonPath("$.interviewDays").value(3));
    }

    @Test
    void updatesSettings() throws Exception {
        JobAttentionSettingsRequest request = new JobAttentionSettingsRequest(12, 6, 2);
        when(service.updateSettings(request))
                .thenReturn(new JobAttentionSettingsResponse(12, 6, 2));

        mockMvc.perform(put("/api/job-activities/attention-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appliedDays": 12,
                                  "onlineAssessmentDays": 6,
                                  "interviewDays": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedDays").value(12))
                .andExpect(jsonPath("$.onlineAssessmentDays").value(6))
                .andExpect(jsonPath("$.interviewDays").value(2));
    }

    @Test
    void rejectsThresholdsOutsideSupportedRange() throws Exception {
        mockMvc.perform(put("/api/job-activities/attention-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appliedDays": 0,
                                  "onlineAssessmentDays": 91,
                                  "interviewDays": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.appliedDays").exists())
                .andExpect(jsonPath("$.fieldErrors.onlineAssessmentDays").exists());

        verifyNoInteractions(service);
    }
}
