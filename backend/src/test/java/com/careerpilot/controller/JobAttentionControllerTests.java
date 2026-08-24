package com.careerpilot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.dto.JobAttentionHistoryResponse;
import com.careerpilot.dto.JobAttentionResponse;
import com.careerpilot.model.JobAttentionEventAction;
import com.careerpilot.model.JobStatus;
import com.careerpilot.service.JobAttentionHistoryService;
import com.careerpilot.service.JobAttentionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobAttentionController.class)
class JobAttentionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobAttentionService jobAttentionService;

    @MockitoBean
    private JobAttentionHistoryService jobAttentionHistoryService;

    @Test
    void returnsJobsNeedingAttention() throws Exception {
        when(jobAttentionService.getJobsNeedingAttention()).thenReturn(List.of(
                new JobAttentionResponse(
                        1L,
                        "OpenAI",
                        "Engineer",
                        JobStatus.APPLIED,
                        Instant.parse("2026-08-10T12:00:00Z"),
                        13L,
                        7
                )
        ));

        mockMvc.perform(get("/api/job-activities/needs-attention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].company").value("OpenAI"))
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andExpect(jsonPath("$[0].daysWithoutActivity").value(13))
                .andExpect(jsonPath("$[0].thresholdDays").value(7));
    }

    @Test
    void returnsRecentAttentionHistory() throws Exception {
        when(jobAttentionHistoryService.getRecentHistory()).thenReturn(List.of(
                new JobAttentionHistoryResponse(
                        7L,
                        1L,
                        "OpenAI",
                        "Engineer",
                        JobAttentionEventAction.RESCHEDULED,
                        LocalDate.parse("2026-08-30"),
                        LocalDate.parse("2026-09-02"),
                        Instant.parse("2026-08-24T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/job-activities/attention-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].company").value("OpenAI"))
                .andExpect(jsonPath("$[0].action").value("RESCHEDULED"))
                .andExpect(jsonPath("$[0].previousSnoozedUntil").value("2026-08-30"))
                .andExpect(jsonPath("$[0].newSnoozedUntil").value("2026-09-02"));
    }
}
